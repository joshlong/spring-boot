package sample.atomikos;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.AtomikosConnectionFactoryBean;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.jms.ConnectionFactory;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Demonstrates how to use Atomikos and JTA
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleAtomikosApplication {

    @Configuration
    @AutoConfigureAfter(DataSourceAutoConfiguration.class)
    public static class HibernateJpaJtaAutoConfiguration implements BeanFactoryAware {

        private BeanFactory beanFactory;

        private String[] getPackagesToScan() {
            if (AutoConfigurationPackages.has(this.beanFactory)) {
                List<String> basePackages = AutoConfigurationPackages.get(this.beanFactory);
                return basePackages.toArray(new String[basePackages.size()]);
            }
            return new String[0];
        }

        private Map<String, String> getVendorProperties(DataSource dataSource, JpaProperties jpaProperties) {
            Map<String, String> vendorProperties = jpaProperties.getHibernateProperties(dataSource);
            vendorProperties.put("hibernate.transaction.jta.platform", SampleAtomikosApplication.AtomikosJtaPlatform.class.getName());
            vendorProperties.put("javax.persistence.transactionType", "JTA");
            return vendorProperties;
        }

        @Bean
        @Primary
        @ConditionalOnMissingBean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, JpaProperties jpaProperties, EntityManagerFactoryBuilder factory) {
            Map<String, String> vals = getVendorProperties(dataSource, jpaProperties);
            return factory.dataSource(dataSource).packages(getPackagesToScan())
                    .properties(vals).build();
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }
    }


    public static class AtomikosJtaPlatform extends AbstractJtaPlatform {

        private static final long serialVersionUID = 1L;
        private static TransactionManager transactionManager;
        private static UserTransaction transaction;

        static void transaction(UserTransaction transaction) {
            AtomikosJtaPlatform.transaction = transaction;
        }

        static void transactionManager(TransactionManager transactionManager) {
            AtomikosJtaPlatform.transactionManager = transactionManager;
        }

        @Override
        protected TransactionManager locateTransactionManager() {
            return transactionManager;
        }

        @Override
        protected UserTransaction locateUserTransaction() {
            return transaction;
        }
    }


    public static final Logger logger = LoggerFactory.getLogger(SampleAtomikosApplication.class);

    private static Properties wellKnownAtomikosProperties(ConfigurableEnvironment environment) {
        List<String> wellKnownAtomikosSystemProperties = Arrays.asList(
                "com.atomikos.icatch.automatic_resource_registration",
                "com.atomikos.icatch.client_demarcation",
                "com.atomikos.icatch.threaded_2pc",
                "com.atomikos.icatch.serial_jta_transactions",
                "com.atomikos.icatch.serializable_logging",
                "com.atomikos.icatch.log_base_dir",
                "com.atomikos.icatch.max_actives",
                "com.atomikos.icatch.checkpoint_interval",
                "com.atomikos.icatch.enable_logging",
                "com.atomikos.icatch.output_dir",
                "com.atomikos.icatch.log_base_name",
                "com.atomikos.icatch.max_timeout",
                "com.atomikos.icatch.tm_unique_name",
                "java.naming.factory.initial",
                "java.naming.provider.url",
                "com.atomikos.icatch.service",
                "com.atomikos.icatch.force_shutdown_on_vm_exit");


        Properties properties = new Properties();
        for (String k : wellKnownAtomikosSystemProperties) {
            if (environment.containsProperty(k)) {
                properties.setProperty(k, environment.getProperty(k));
            }
        }
        return properties;
    }

    private static javax.jms.XAConnectionFactory connectionFactory(String url) {
        return new ActiveMQXAConnectionFactory(url);
    }

    private static javax.sql.XADataSource dataSource(String host, String db, String username, String pw) {
        PGXADataSource pgxaDataSource = new PGXADataSource();
        pgxaDataSource.setServerName(host);
        pgxaDataSource.setDatabaseName(db);
        pgxaDataSource.setUser(username);
        pgxaDataSource.setPassword(pw);
        return pgxaDataSource;
    }

    private static Map<String, Object> atomikosRootFileProperties(ConfigurableEnvironment env, File file) {
        String rootFileName = file.getAbsolutePath();
        String[] propsForRootDirectory = {"com.atomikos.icatch.log_base_dir", "com.atomikos.icatch.output_dir"};
        Map<String, Object> stringObjectMap = new HashMap<String, Object>();
        for (String p : propsForRootDirectory)
            stringObjectMap.put(p, rootFileName);
        return stringObjectMap;
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleAtomikosApplication.class, args);
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosConnectionFactoryBean xaConnectionFactory() {
        AtomikosConnectionFactoryBean xaCF = new AtomikosConnectionFactoryBean();
        xaCF.setXaConnectionFactory(connectionFactory("tcp://localhost:61616"));
        xaCF.setUniqueResourceName("xaConnectionFactory");
        xaCF.setPoolSize(10);
        return xaCF;
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean xaDataSource() {
        logger.info("creating xaDataSource...");
        AtomikosDataSourceBean xaDS = new AtomikosDataSourceBean();
        xaDS.setUniqueResourceName("xaDataSource");
        xaDS.setTestQuery("select now()");
        xaDS.setXaDataSource(dataSource("127.0.0.1", "crm", "crm", "crm"));
        xaDS.setPoolSize(10);
        return xaDS;
    }


    @Bean(initMethod = "init", destroyMethod = "shutdownForce")
    @ConditionalOnMissingBean
    public com.atomikos.icatch.config.UserTransactionServiceImp userTransactionService(ConfigurableEnvironment environment) {

        File file = new File("/Users/jlong/Desktop/atomikos/");
        if (file.exists()) file.delete();
        file.mkdirs();

        Map<String, Object> rootFiles = atomikosRootFileProperties(environment, file);
        EnvironmentUtils.addEnvironmentProperty(environment, rootFiles);
        Properties properties = wellKnownAtomikosProperties(environment);
        return new com.atomikos.icatch.config.UserTransactionServiceImp(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(JpaVendorAdapter jpaVendorAdapter,
                                                                           DataSource dataSource) throws Throwable {


        HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put("hibernate.transaction.jta.platform", AtomikosJtaPlatform.class.getName());
        properties.put("javax.persistence.transactionType", "JTA");

        LocalContainerEntityManagerFactoryBean entityManager = new LocalContainerEntityManagerFactoryBean();
        entityManager.setJtaDataSource(dataSource);
        entityManager.setJpaVendorAdapter(jpaVendorAdapter);
        entityManager.setPackagesToScan("com.at.mul.domain.order");
        entityManager.setPersistenceUnitName("orderPersistenceUnit");
        entityManager.setJpaPropertyMap(properties);
        return entityManager;
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    @ConditionalOnMissingBean
    public com.atomikos.icatch.jta.UserTransactionManager atomikosUserTransactionManager() throws SystemException {
        UserTransactionManager userTransactionManager = new UserTransactionManager();

        AtomikosJtaPlatform.transactionManager(userTransactionManager);

        return userTransactionManager;
    }

    @Bean
    @ConditionalOnMissingBean
    public UserTransactionImp atomikosUserTransaction() throws SystemException {
        UserTransactionImp userTransactionImp = new UserTransactionImp();
        userTransactionImp.setTransactionTimeout(10000);

        AtomikosJtaPlatform.transaction(userTransactionImp);

        return userTransactionImp;
    }

    @Bean
    @ConditionalOnMissingBean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setSessionTransacted(true);
        jmsTemplate.setReceiveTimeout(100);
        return jmsTemplate;
    }

    @Bean(name = "transactionManager")
    @ConditionalOnMissingBean(value = PlatformTransactionManager.class)
    public JtaTransactionManager transactionManager(com.atomikos.icatch.jta.UserTransactionManager atomikosTransactionManager,
                                                    com.atomikos.icatch.jta.UserTransactionImp atomikosUserTransaction) {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(atomikosUserTransaction, atomikosTransactionManager);
        jtaTransactionManager.setAllowCustomIsolationLevels(true);

        // make sure the JtaPlatform is working
        AtomikosJtaPlatform.transaction ( atomikosUserTransaction);
        AtomikosJtaPlatform.transactionManager ( atomikosTransactionManager);

        return jtaTransactionManager;
    }

    @Bean
    public CommandLineRunner init(final JdbcTemplate jdbcTemplate, final AccountService accountService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {

                jdbcTemplate.execute("delete from account");

                logger.info(accountService.createAccount("pwebb", false).toString());
                logger.info(accountService.createAccount("dsyer", false).toString());
                logger.info(accountService.createAccount("jlong", true).toString());

                List<Account> accountList = jdbcTemplate.query(
                        "select * from account", new RowMapper<Account>() {
                            @Override
                            public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
                                return new Account(rs.getLong("id"), rs.getString("username"));
                            }
                        });

                for (Account account : accountList) {
                    logger.info("account " + account.toString());
                }


            }
        };
    }
}

class EnvironmentUtils {
    static void addEnvironmentProperty(ConfigurableEnvironment environment, Map<String, Object> props) {
        environment.getPropertySources().addFirst(new MapPropertySource("jta", props));
    }

}

@Service
class AccountService {

    private static Logger logger = LoggerFactory.getLogger(AccountService.class);

    private JmsTemplate jmsTemplate;
    private AccountRepository accountRepository;

    @Autowired
    AccountService(AccountRepository accountRepository,
                   JmsTemplate jmsTemplate) {
        this.accountRepository = accountRepository;
        this.jmsTemplate = jmsTemplate;
    }


    @Transactional
    public Account createAccount(String username, boolean rollback) {
        Account account = this.accountRepository.save(new Account(username));
        logger.info("created account " + account.toString());
        String msg = account.getId() + ":" + account.getUsername();
        jmsTemplate.convertAndSend("accounts", msg);
        logger.info("send message to 'accounts' destination " + msg);
        if (rollback) {
            String err = "throwing an exception for account#" + account.getId() +
                    ". This record should not be visible in the DB or in JMS.";
            logger.info(err);
            throw new IllegalStateException(err);
        }
        return account;
    }


}

@Entity
class Account {
    @Id
    @GeneratedValue
    Long id;
    String username;

    Account() {
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Account{");
        sb.append("id=").append(id);
        sb.append(", username='").append(username).append('\'');
        sb.append('}');
        return sb.toString();
    }

    Account(String username) {
        this.username = username;
    }

    Account(Long id, String username) {
        this.username = username;
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}

interface AccountRepository extends JpaRepository<Account, Long> {
}

@Configuration
@ConditionalOnClass({
        com.atomikos.icatch.jta.hibernate3.TransactionManagerLookup.class,
        LocalContainerEntityManagerFactoryBean.class,
        EnableTransactionManagement.class, EntityManager.class})
// @Conditional(HibernateJpaAutoConfiguration.HibernateEntityManagerCondition.class)
// @AutoConfigureAfter(DataSourceAutoConfiguration.class)
// @AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
class HibernateAtomikosJtaAutoConfiguration {


}

/*
@Configuration
@AutoConfigureBefore({DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@Conditional(JtaAutoConfiguration.JtaCondition.class)
@ConditionalOnClass(JtaTransactionManager.class)
class JtaAutoConfiguration {

    private static Log logger = LogFactory.getLog(JtaAutoConfiguration.class);


    // configure the third party Atomikos JTA support
    @Configuration
    @ConditionalOnClass(com.atomikos.icatch.jta.UserTransactionImp.class)
    public static class AtomikosJtaAutoConfiguration {

    }

    @Configuration
    @Conditional(JavaEeEnvironmentJtaCondition.class)
    public static class JavaEeJtaAutoConfiguration {

        @Bean(name = "transactionManager")
        @ConditionalOnMissingBean(value = PlatformTransactionManager.class)
        public JtaTransactionManager transactionManager() {
            return new JtaTransactionManager();
        }
    }


    public static class JavaEeEnvironmentJtaCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            try {
                JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
                jtaTransactionManager.setAutodetectTransactionManager(true);
                jtaTransactionManager.setAutodetectUserTransaction(true);
                jtaTransactionManager.afterPropertiesSet();
                // made it this far, so the setup should've succeeded.
                return ConditionOutcome.match();
            } catch (IllegalStateException e) {
                return ConditionOutcome.noMatch("couldn't initialize a " +
                        JtaTransactionManager.class.getName() + " correctly.");
            }
        }
    }


    public static class JtaCondition extends SpringBootCondition {

        private static String[] CLASS_NAMES =
                "javax.jms.XAConnectionFactory,javax.sql.XADataSource".split(",");

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            // basic test for JTA classes
            for (String className : CLASS_NAMES) {
                if (ClassUtils.isPresent(className, context.getClassLoader())) {
                    return ConditionOutcome.match("found an XA class on the CLASSPATH.");
                }
            }
            return ConditionOutcome.noMatch("no XA class.");
        }
    }
}

*/

