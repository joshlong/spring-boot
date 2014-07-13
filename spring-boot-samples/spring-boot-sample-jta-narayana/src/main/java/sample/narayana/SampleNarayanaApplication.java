package sample.narayana;

import com.arjuna.ats.internal.jdbc.DynamicClass;
import com.arjuna.ats.jdbc.TransactionalDriver;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demonstrates how to use Atomikos and JTA together to coordinate a transaction database connection (to PostgreSQL)
 * and a transactional Message Queue connection (to ActiveMQ, in this case)
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration//(exclude = JtaAutoConfiguration.class)
public class SampleNarayanaApplication {

    private static Map<String, XADataSource> XA_DATA_SOURCE_MAP =
            new ConcurrentHashMap<String, XADataSource>();

    public static class SpringDynamicClass implements DynamicClass {
        @Override
        public XADataSource getDataSource(String dataSourceBeanName) throws SQLException {
            return XA_DATA_SOURCE_MAP.get(dataSourceBeanName);
        }
    }

    private static javax.sql.XADataSource dataSource(String host, String db, String username, String pw) {
        PGXADataSource pgxaDataSource = new PGXADataSource();
        pgxaDataSource.setServerName(host);
        pgxaDataSource.setDatabaseName(db);
        pgxaDataSource.setUser(username);
        pgxaDataSource.setPassword(pw);
        return pgxaDataSource;
    }

    /**
     * this is how the sausage is made: we build an XADataSource,
     * wrap it, and then return a pool to that proxy.
     */
    @Bean
    public FactoryBean<DataSource> dataSource() {
        XADataSource xaDataSource = dataSource("127.0.0.1", "crm", "crm", "crm");
        return new NarayanaDataSourceFactoryBean(xaDataSource, "crm", "crm");
    }


    public static class NarayanaDataSourceFactoryBean
            implements BeanNameAware,
            FactoryBean<DataSource> {

        private String beanName;
        private final XADataSource xaDataSource;
        private String user, password;

        private static DataSource buildXaDataSource(
                String user,
                String pw,
                XADataSource xaDataSource,
                String beanName) {
            XA_DATA_SOURCE_MAP.put(beanName, xaDataSource);
            TransactionalDriver transactionalDriver = new TransactionalDriver();
            Properties properties = new Properties();
            properties.setProperty(TransactionalDriver.userName, user);
            properties.setProperty(TransactionalDriver.password, pw);
            properties.setProperty(TransactionalDriver.dynamicClass, SpringDynamicClass.class.getName());
            String url = TransactionalDriver.arjunaDriver + "" + beanName;
            SimpleDriverDataSource simpleDriverDataSource =
                    new SimpleDriverDataSource(transactionalDriver, url, properties);
            simpleDriverDataSource.setUsername(user);
            simpleDriverDataSource.setPassword(pw);
            return simpleDriverDataSource;
        }

        @Override
        public void setBeanName(String name) {
            this.beanName = name;
        }

        public NarayanaDataSourceFactoryBean(
                XADataSource xaDataSource, String username, String password) {
            this.xaDataSource = xaDataSource;
            this.user = username;
            this.password = password;
        }

        @Override
        public DataSource getObject() throws Exception {
            return buildXaDataSource(this.user, this.password, this.xaDataSource, this.beanName);
        }

        @Override
        public Class<?> getObjectType() {
            return DataSource.class;
        }

        @Override
        public boolean isSingleton() {
            return true;
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleNarayanaApplication.class, args);
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    private RowMapper<Account> accountRowMapper = new RowMapper<Account>() {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Account(rs.getLong("id"), rs.getString("username"));
        }
    };

    @Bean
    public CommandLineRunner init(
            final AccountService accountService,
            final PlatformTransactionManager transactionManager,
            final JdbcTemplate jdbcTemplate) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                l.info("transactionManager: " + transactionManager.toString());
                listAccounts();
                accountService.createAccount("jlong", false);
                listAccounts();
                accountService.createAccount("pwebb", true);
                listAccounts();
            }

            private Logger l = LoggerFactory.getLogger(getClass());

            private void listAccounts() {

                List<Account> accountList = jdbcTemplate.query("select * from account", accountRowMapper);
                for (Account account : accountList) {
                    l.info(account.toString());
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
    AccountService(AccountRepository accountRepository
                  /* JmsTemplate jmsTemplate*/) {
        this.accountRepository = accountRepository;
        this.jmsTemplate = jmsTemplate;
    }


    @Transactional
    public Account createAccount(String username, boolean rollback) {

        Account account = this.accountRepository.save(new Account(username));
        String msg = account.getId() + ":" + account.getUsername();

        ///jmsTemplate.convertAndSend("accounts", msg);

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


@Entity
class Account {
    @Id
    @GeneratedValue
    private long id;
    private String username;

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    Account(String username) {
        this.username = username;
    }

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

    public Account(long id, String username) {
        this.id = id;
        this.username = username;
    }
}