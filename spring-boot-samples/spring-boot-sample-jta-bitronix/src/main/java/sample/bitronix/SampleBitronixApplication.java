package sample.bitronix;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jta.bitronix.BitronixXaConnectionFactory;
import org.springframework.boot.autoconfigure.jta.bitronix.BitronixXaDataSourceFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    public static void wrench(String msg) {
        throw new RuntimeException("Monkey wrench! " + msg);
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

    protected String[] names(String prefix, String... us) {
        List<String> strings = new ArrayList<String>();
        for (String u : us)
            strings.add(prefix + u);
        return strings.toArray(new String[strings.size()]);
    }

    @Autowired
    public void announcedJtaTransactionManager(JtaTransactionManager transactionManager) {
        System.out.println(PlatformTransactionManager.class.getName() + " = " + transactionManager);
    }

    @Bean
    public CommandLineRunner clean(final JdbcTemplate jdbcTemplate) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                jdbcTemplate.execute("delete from account");
            }
        };
    }

    @Bean
    public CommandLineRunner jpa(final JpaAccountService accountService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                System.out.println();
                System.out.println("JPA");
                for (Account a : accountService.createAccounts(names("JPA ", "jlong", "mgray", "mchang"))) {
                    System.out.println("created account: " + a.toString());
                    System.out.println("returned account from DB query: " +
                            accountService.readAccount(a.getId()));
                }

            }
        };
    }

    @Bean
    public CommandLineRunner jdbc(final JdbcAccountService accountService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                System.out.println();
                System.out.println("JDBC");
                for (Account a : accountService.createAccounts(names("JDBC", "jlong", "mgray", "mchang"))) {
                    System.out.println("created account: " + a.toString());
                    System.out.println("returned account from DB query: " +
                            accountService.readAccount(a.getId()));
                }
            }
        };
    }
}

interface AccountRepository extends JpaRepository<Account, Long> {
}

@Service
class JpaAccountService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AccountRepository accountRepository;

    @Autowired
    public JpaAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void deleteAllAccounts() {
        this.accountRepository.deleteAllInBatch();
    }

    @Transactional(readOnly = true)
    public Account readAccount(long id) {
        return this.accountRepository.findOne(id);
    }

    @Transactional
    public List<Account> createAccounts(String... usernames) {
        List<Account> accounts = new ArrayList<Account>();
        for (String u : usernames)
            accounts.add(this.createAccount(u));
        return accounts;
    }

    @Transactional
    public Account createAccount(String username) {
        return this.accountRepository.save(new Account(username));
    }

    @Transactional(readOnly = true)
    public void iterateAccounts() {
        logger.info("---------------------------------------------------------------");
        logger.info("Iterating all the " + Account.class.getName() + "s.");
        logger.info("---------------------------------------------------------------");
        for (Account account : readAccounts()) {
            logger.info("account " + account.toString());
        }
        logger.info("---------------------------------------------------------------");
    }


    @Transactional(readOnly = true)
    public Collection<Account> readAccounts() {
        return this.accountRepository.findAll();
    }

}

@Service
class JdbcAccountService {

    private final JdbcTemplate jdbcTemplate;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RowMapper<Account> accountRowMapper = new RowMapper<Account>() {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Account(rs.getLong("id"), rs.getString("username"));
        }
    };

    @Autowired
    public JdbcAccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void deleteAllAccounts() {
        this.jdbcTemplate.update("delete from account ");
    }

    @Transactional(readOnly = true)
    public void iterateAccounts() {
        logger.info("---------------------------------------------------------------");
        logger.info("Iterating all the " + Account.class.getName() + "s.");
        logger.info("---------------------------------------------------------------");
        for (Account account : readAccounts()) {
            logger.info("account " + account.toString());
        }
        logger.info("---------------------------------------------------------------");
    }


    public Account readAccount(long id) {
        return this.jdbcTemplate.queryForObject(
                "select * from account where id = ? ", this.accountRowMapper, (Object) id);
    }

    @Transactional
    public Account createAccount(final String username) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreatorFactory stmtFactory = new PreparedStatementCreatorFactory(
                "insert into account(id, username) values (nextval('hibernate_sequence'), ?)", new int[]{Types.VARCHAR});
        stmtFactory.setGeneratedKeysColumnNames(new String[]{"id"});
        PreparedStatementCreator psc = stmtFactory.newPreparedStatementCreator(Arrays.asList(username));
        jdbcTemplate.update(psc, keyHolder);
        Number newAccountId = keyHolder.getKey();
        return this.readAccount(newAccountId.longValue());
    }

    @Transactional
    public List<Account> createAccounts(String... usernames) {
        List<Account> accountList = new ArrayList<Account>();
        for (String username : usernames)
            accountList.add(this.createAccount(username));


        return accountList;
    }

    @Transactional(readOnly = true)
    public Collection<Account> readAccounts() {
        return jdbcTemplate.query("select * from account", this.accountRowMapper);
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
