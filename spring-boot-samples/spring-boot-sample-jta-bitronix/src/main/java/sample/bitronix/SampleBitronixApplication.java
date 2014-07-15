package sample.bitronix;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.jms.XAConnectionFactory;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.XADataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demonstrates how to use Atomikos and JTA together to coordinate a transaction database connection (to PostgreSQL)
 * and a transactional Message Queue connection (to ActiveMQ, in this case)
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleBitronixApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleBitronixApplication.class, args);
    }

    @Bean
    public FactoryBean<PoolingConnectionFactory> poolingConnectionFactory() {
        return new BitronixXaConnectionFactory<ActiveMQXAConnectionFactory>(ActiveMQXAConnectionFactory.class) {
            @Override
            protected void configureXaConnectionFactory(ActiveMQXAConnectionFactory xaDataSource) {
                xaDataSource.setBrokerURL("tcp://localhost:61616");
            }
        };
    }

    @Bean
    public FactoryBean<PoolingDataSource> poolingDataSource() {
        return new BitronixXaDataSourceFactoryBean<PGXADataSource>(PGXADataSource.class) {
            @Override
            protected void configureXaDataSource(PGXADataSource pgxaDataSource) {
                pgxaDataSource.setServerName("127.0.0.1");
                pgxaDataSource.setDatabaseName("crm");
                pgxaDataSource.setUser("crm");
                pgxaDataSource.setPassword("crm");
            }
        };
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    @Bean
    public CommandLineRunner init(final JdbcTemplate jdbcTemplate,
                                  final JtaTransactionManager jtaTransactionManager,
                                  final AccountService accountService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {

                Logger logger = LoggerFactory.getLogger(getClass());

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

abstract class BitronixXaConnectionFactory<T extends XAConnectionFactory>
        implements BeanNameAware, FactoryBean<PoolingConnectionFactory> {
    private final Class<T> xaDataSourceClass;
    private String uniqueNodeName;

    public BitronixXaConnectionFactory(Class<T> xaDataSourceClass) {
        this.xaDataSourceClass = xaDataSourceClass;
    }

    protected PoolingConnectionFactory buildXaConnectionFactory(
            String uniqueNodeName, Class<T> xaDataSourceClassName)
            throws IllegalAccessException, InstantiationException {

        PoolingConnectionFactory poolingDataSource = new PoolingConnectionFactory();

        Map<String, Object> recordedProperties = new ConcurrentHashMap<String, Object>();
        T recordingDataSource = PropertyRecordingProxyUtils.buildPropertyRecordingConnectionFactory(xaDataSourceClassName, recordedProperties);
        configureXaConnectionFactory(recordingDataSource);

        poolingDataSource.setClassName(xaDataSourceClassName.getName());
        poolingDataSource.setMaxPoolSize(10);
        poolingDataSource.setUniqueName(uniqueNodeName);
        poolingDataSource.getDriverProperties().putAll(recordedProperties);
        poolingDataSource.init();

        return poolingDataSource;
    }

    @Override
    public void setBeanName(String name) {
        this.uniqueNodeName = name + Long.toString(System.currentTimeMillis());
    }

    @Override
    public PoolingConnectionFactory getObject() throws Exception {
        return this.buildXaConnectionFactory(this.uniqueNodeName, this.xaDataSourceClass);
    }

    @Override
    public Class<?> getObjectType() {
        return PoolingConnectionFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * client will be given a proxy to the instance which will record property configurations
     * and pass them through to the underlying {@link javax.jms.XAConnectionFactory}
     */
    protected abstract void configureXaConnectionFactory(T xaDataSource);
}

/**
 * the Bitronix connectivity depends on accessing the transactional resource <em>behind</em>
 * the provided {@link bitronix.tm.resource.jms.PoolingConnectionFactory}
 * and {@link bitronix.tm.resource.jdbc.PoolingDataSource} implementations that they provide.
 * <p>
 * Configuring the underlying {@link javax.sql.XADataSource} is annoying.
 * All properties on the underlying {@link javax.sql.XADataSource} must be configured
 * as a {@link java.util.Properties} on the wrapper {@link bitronix.tm.resource.jdbc.PoolingDataSource}.
 * <p>
 * This lets you configure a native proxy which then gets mapped to the underlying properties.
 * Less fat-fingering.
 *
 * @param <T>
 */
abstract class BitronixXaDataSourceFactoryBean<T extends XADataSource>
        implements BeanNameAware, FactoryBean<PoolingDataSource> {

    private final Class<T> xaDataSourceClass;
    private String uniqueNodeName;

    public BitronixXaDataSourceFactoryBean(Class<T> xaDataSourceClass) {
        this.xaDataSourceClass = xaDataSourceClass;
    }

    @Override
    public PoolingDataSource getObject() throws Exception {
        return this.buildXaDataSource(
                this.uniqueNodeName,
                this.xaDataSourceClass);
    }

    @Override
    public Class<?> getObjectType() {
        return PoolingDataSource.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setBeanName(String name) {
        this.uniqueNodeName = name + Long.toString(System.currentTimeMillis());
    }


    protected PoolingDataSource buildXaDataSource(
            String uniqueNodeName, Class<T> xaDataSourceClassName)
            throws IllegalAccessException, InstantiationException {

        PoolingDataSource poolingDataSource = new PoolingDataSource();

        Map<String, Object> recordedProperties = new ConcurrentHashMap<String, Object>();
        T recordingDataSource = PropertyRecordingProxyUtils.buildPropertyRecordingXaDataSource(
                xaDataSourceClassName, recordedProperties);
        configureXaDataSource(recordingDataSource);

        poolingDataSource.setClassName(xaDataSourceClassName.getName());
        poolingDataSource.setMaxPoolSize(10);
        poolingDataSource.getDriverProperties().putAll(recordedProperties);
        poolingDataSource.setUniqueName(uniqueNodeName);
        poolingDataSource.init();

        return poolingDataSource;
    }

    protected abstract void configureXaDataSource(T xaDataSource);
}

abstract class PropertyRecordingProxyUtils {

    private static void record(MethodInvocation methodInvocation, Map<String, Object> holder) {
        String setterPrefix = "set";
        String methodInvName = methodInvocation.getMethod().getName();
        if (methodInvName.startsWith(setterPrefix)) {
            String propertyName = methodInvName.substring(setterPrefix.length());
            propertyName = Character.toLowerCase(propertyName.charAt(0)) +
                    propertyName.substring(1);
            Object[] args = methodInvocation.getArguments();
            if (args.length == 1) {
                holder.put(propertyName, args[0]);
            }
        }
    }

    private static MethodInterceptor recordingInterceptor(final Map<String, Object> properties) {

        return new MethodInterceptor() {
            @Override
            public Object invoke(MethodInvocation methodInvocation) throws Throwable {

                record(methodInvocation, properties);

                if (methodInvocation.getMethod().getName().equals("toString"))
                    return "a property recording proxy around a XA resource for Bitronix. " +
                            "All calls to this proxy will be ignored and are only " +
                            "to facilitate configuring a Bitronix-enlisted XA resource.";

                return null;
            }
        };
    }

    private static <T> T resource(final Class<T> clzz, final Map<String, Object> holderForProperties) {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTargetClass(clzz);
        proxyFactoryBean.setProxyTargetClass(true);
        proxyFactoryBean.setAutodetectInterfaces(true);
        proxyFactoryBean.addAdvice(
                recordingInterceptor(holderForProperties));
        return (T) proxyFactoryBean.getObject();
    }

    public static <MQ extends XAConnectionFactory> MQ buildPropertyRecordingConnectionFactory(
            final Class<MQ> clzz, final Map<String, Object> holderForProperties) {
        return resource(clzz, holderForProperties);
    }

    public static <DS extends XADataSource> DS buildPropertyRecordingXaDataSource(
            final Class<DS> clzz, final Map<String, Object> holderForProperties) {
        return resource(clzz, holderForProperties);
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
        String msg = account.getId() + ":" + account.getUsername();

        jmsTemplate.convertAndSend("accounts", msg);

        logger.info("created account " + account.toString());


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

interface AccountRepository extends JpaRepository<Account, Long> {
}
