package sample.atomikos;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.AtomikosConnectionFactoryBean;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.ConnectionFactory;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Demonstrates how to use Atomikos and JTA
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleAtomikosApplication {

    public static final Logger logger = LoggerFactory.getLogger(SampleAtomikosApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SampleAtomikosApplication.class, args);
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean xaDataSource() {
        AtomikosDataSourceBean xaDS = new AtomikosDataSourceBean();
        xaDS.setUniqueResourceName("xaDataSource");
        xaDS.setTestQuery("select now()");
        xaDS.setXaDataSource(dataSource("127.0.0.1", "crm", "crm", "crm"));
        xaDS.setPoolSize(10);
        return xaDS;
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosConnectionFactoryBean xaConnectionFactory() {
        AtomikosConnectionFactoryBean xaCF = new AtomikosConnectionFactoryBean();
        xaCF.setXaConnectionFactory(connectionFactory("tcp://localhost:61616"));
        xaCF.setUniqueResourceName("xaConnectionFactory");
        xaCF.setPoolSize(10);
        return xaCF;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setSessionTransacted(true);
        jmsTemplate.setReceiveTimeout(100);
        return jmsTemplate;
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

