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
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Demonstrates how to use Atomikos and JTA together to coordinate a transaction database connection (to PostgreSQL)
 * and a transactional Message Queue connection (to ActiveMQ, in this case)
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration(exclude = ActiveMQAutoConfiguration.class)
public class SampleAtomikosApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                SampleAtomikosApplication.class, args);
    }

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosDataSourceBean xaDataSource() {

        AtomikosDataSourceBean xa = new AtomikosDataSourceBean();
        xa.setXaDataSource(dataSource("127.0.0.1", "crm", "crm", "crm"));
        xa.setUniqueResourceName("xaDataSource");
        xa.setTestQuery("select now()");
        xa.setPoolSize(this.poolSize);
        return xa;
    }

    int poolSize = 10;

    @Bean(initMethod = "init", destroyMethod = "close")
    public AtomikosConnectionFactoryBean xaConnectionFactory() {
        AtomikosConnectionFactoryBean xa = new AtomikosConnectionFactoryBean();
        xa.setXaConnectionFactory(connectionFactory("tcp://localhost:61616"));
        xa.setUniqueResourceName("xaConnectionFactory");
        xa.setPoolSize(this.poolSize);
        return xa;
    }

    private static javax.jms.XAQueueConnectionFactory connectionFactory(String url) {
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
    public TransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    @Bean
    public CommandLineRunner init(
            final TransactionTemplate transactionTemplate,
            final JdbcTemplate jdbcTemplate,
            final AccountService accountService) {
        return new CommandLineRunner() {

            private final Logger logger = LoggerFactory.getLogger(getClass());


            protected void execute() {
                jdbcTemplate.execute("delete from account");

                logger.info(accountService.createAccount("pwebb", false).toString());
                logger.info(accountService.createAccount("dsyer", false).toString());
                logger.info(accountService.createAccount("jlong", false).toString());

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


            private boolean allTogether = false;


            @Override
            public void run(String... args) throws Exception {

                if (!this.allTogether) {
                    execute();
                } else {
                    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            execute();
                        }
                    });
                }
            }
        };

    }
}


@Service
class AccountService {

    private static Logger logger =
            LoggerFactory.getLogger(AccountService.class);

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

        jmsTemplate.convertAndSend("accounts", "hello, world!");

        Account account = this.accountRepository.save(new Account(username));
        String msg = account.getId() + ":" + account.getUsername();


        logger.info("created account " + account.toString());
        logger.info("send message to 'accounts' destination " + msg);

        if (rollback) {
            String err = "throwing an exception for account # " + account.getId() +
                    ". This record should not be visible in the DB or in JMS.";
            logger.info(err);
            throw new IllegalStateException(err);
        }

        this.accountRepository.flush();
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
