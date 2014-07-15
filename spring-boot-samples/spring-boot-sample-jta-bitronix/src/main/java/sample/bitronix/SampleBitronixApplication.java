package sample.bitronix;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jta.bitronix.BitronixXaConnectionFactory;
import org.springframework.boot.autoconfigure.jta.bitronix.BitronixXaDataSourceFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.PreparedStatement;
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
    public CommandLineRunner init(final TransactionTemplate transactionTemplate,
                                  final JdbcTemplate jdbcTemplate) {
        return new CommandLineRunner() {

            private Logger logger =  LoggerFactory.getLogger(getClass());

            private void iterate (){

                logger.info( "Iterating all the " + Account.class.getName() + "s.");
                logger.info( "-----------------------------------------------");
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
                logger.info( "-----------------------------------------------");
            }
            @Override
            public void run(String... args) throws Exception {

                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        jdbcTemplate.update("delete from account");
                    }
                });

                transactionTemplate.execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                iterate();
                                for (final String u : "jlong,jwoo,mchang,mgray".split(",")) {
                                    jdbcTemplate.update(
                                            "insert into account(id, username) " +
                                                    " values (nextval('hibernate_sequence'), ?)",
                                            new PreparedStatementSetter() {
                                                @Override
                                                public void setValues(PreparedStatement ps) throws SQLException {
                                                    ps.setString(1, u);
                                                }
                                            });
                                }
                                iterate();

                                throw new RuntimeException("Monkey wrench!");
                            }
                        });
            }
        };
    }
/*
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


            }
        };
    }*/
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


/*

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
*/
