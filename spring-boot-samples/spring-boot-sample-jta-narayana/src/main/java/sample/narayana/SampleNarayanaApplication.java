package sample.narayana;

import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jta.NarayanaDataSourceFactoryBean;
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
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


/**
 * Interesting guide - http://jbossts.blogspot.com/2012/01/connecting-dots.html :-)
 * <p>
 * The roles, responsibilities and relationship between the JTA and JCA components of an app
 * server are critical considerations when you're trying to do without one. The JTA manages transaction lifecycle - begin/commit/rollback.
 * Most importantly, it make appropriate calls on any enlisted XAResources as the transaction progresses. But here is the bit that most users don't
 * pay attention to: A JTA does not magically know what resources you want to participate in the transaction. Telling it that is the JCA's job.
 * <p>
 * In a full on app server, the JCA manages connections to resource managers such as databases and message queues.
 * If you deploy those drivers/connectors in a manner that identifies them as XA enabled, the JCA ensures that they
 * are correctly associated with the transaction. Application code simply e.g. looks up the JNDI name for a connection pool and calls
 * getConnection(). The JCA intercepts the call, get the XAResource for the connection and passes it to the transaction manager.
 * <p>
 * In some cases you don't need a full JCA. You can often make do with a transaction manager aware XA connection pool,
 * which is essentially a subset of the JCA functionality. But you can't get away with only an XA aware driver or a non-XA connection pool.
 * Trying to do that leads to some interesting behaviour: your app will deploy and run, but you have a transaction and a connection that know nothing
 * about one another. Committing or rolling back the transaction won't commit or rollback the work in the database. oops.
 * <p>
 * So, you also need to wire in a JCA or suitable connection pooling implementation. Most 'standalone' JTA implementations ship with a simple
 * connection management solution that is suitable for light use. The one in JBossTS is called the TransactionalDriver. For serious deployments
 * you want IronJacamar or some other JCA that has robust and fast connection management.
 * <p>
 * So now you have wired up your JTA and JCA in Spring, but you are still not done because...
 * <p>
 * The standard contract between JTA and JCA does not include recovery management setup. Wiring up resources for crash recovery requires
 * a proprietary solution that differs for each transaction manager. The connection manager that ships with the transaction manager may
 * do this more or less automatically, but third party JCAs or XA aware connection pools probably won't.
 * So, go read the transaction manager documentation and write a few test cases.
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleNarayanaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleNarayanaApplication.class, args);
    }

    private static ActiveMQXAConnectionFactory connectionFactory(String url) {
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

    /**
     * this is how the sausage is made: we build an XADataSource,
     * wrap it, and then return a pool to that proxy.
     */
    @Bean
    public FactoryBean<DataSource> dataSource() {
        XADataSource xaDataSource = dataSource("127.0.0.1", "crm", "crm", "crm");
        return new NarayanaDataSourceFactoryBean(xaDataSource, "crm", "crm");
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    @Bean
    public ActiveMQXAConnectionFactory connectionFactory() {
        return connectionFactory("tcp://localhost:61616");
    }

    @Bean
    public CommandLineRunner init(
            final AccountService accountService,
            final PlatformTransactionManager transactionManager,
            final JdbcTemplate jdbcTemplate) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {

                jdbcTemplate.execute("delete from account");

                logger.info("transactionManager: " + transactionManager.toString());
                listAccounts();
                accountService.createAccount("jlong", false);
                listAccounts();
                accountService.createAccount("pwebb", true);
                listAccounts();
            }

            private RowMapper<Account> accountRowMapper = new RowMapper<Account>() {
                @Override
                public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new Account(rs.getLong("id"), rs.getString("username"));
                }
            };

            private Logger logger = LoggerFactory.getLogger(getClass());

            private void listAccounts() {
                List<Account> accountList = jdbcTemplate.query("select * from account", accountRowMapper);
                for (Account account : accountList) {
                    logger.info(account.toString());
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
    AccountService(AccountRepository accountRepository, JmsTemplate jmsTemplate) {
        this.accountRepository = accountRepository;
        this.jmsTemplate = jmsTemplate;
    }


    @Transactional
    public Account createAccount(String username, boolean rollback) {

        Account account = this.accountRepository.save(new Account(username));
        String msg = account.getId() + " " + account.getUsername();

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