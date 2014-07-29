package sample.bitronix;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jta.bitronix.BitronixXaConnectionFactoryFactoryBean;
import org.springframework.boot.autoconfigure.jta.bitronix.BitronixXaDataSourceFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates how to use Bitronix and JTA together to coordinate a transaction database connection (to PostgreSQL)
 * and a transactional Message Queue connection (to ActiveMQ, in this case)
 *
 * @author Josh Long
 */
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleBitronixApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleBitronixApplication.class, args);
    }

    @Bean
    public FactoryBean<PoolingConnectionFactory> connectionFactory() {

        return new BitronixXaConnectionFactoryFactoryBean<ActiveMQXAConnectionFactory>(
                ActiveMQXAConnectionFactory.class) {
            @Override
            protected void configureXaResource(ActiveMQXAConnectionFactory xa) {
                xa.setBrokerURL("tcp://localhost:61616");
            }
        };
    }

    @Bean
    public FactoryBean<PoolingDataSource> dataSource() {
        return new BitronixXaDataSourceFactoryBean<PGXADataSource>(PGXADataSource.class) {
            @Override
            protected void configureXaResource(PGXADataSource xa) {
                xa.setServerName("127.0.0.1");
                xa.setDatabaseName("crm");
                xa.setUser("crm");
                xa.setPassword("crm");
            }
        };
    }

    @Bean
    public CommandLineRunner jpa(final JpaAccountService accountService) {
        return new AccountServiceCommandLineRunner(accountService);
    }

    @Bean
    public CommandLineRunner jdbc(final JdbcAccountService accountService) {
        return new AccountServiceCommandLineRunner(accountService);
    }

}

class AccountServiceCommandLineRunner implements CommandLineRunner, BeanNameAware {

    private final AccountService accountService;
    private Logger logger = LoggerFactory.getLogger(getClass());
    private String prefix;
    private TransactionTemplate transactionTemplate;

    @Autowired
    public void configureTransactionTemplate(JtaTransactionManager txManager) {
        this.transactionTemplate = new TransactionTemplate(txManager);
    }

    @Autowired
    public AccountServiceCommandLineRunner(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info(this.prefix);
        this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                accountService.createAccountAndNotify(prefix + "-jms");
                iterateAccounts("insert");
                status.setRollbackOnly();
            }
        });
        iterateAccounts("after");
    }

    protected void iterateAccounts(String msg) {
        logger.info("---------------------------------------------------------------");
        logger.info("accounts: " + this.prefix + ": " + msg);
        logger.info("---------------------------------------------------------------");
        for (Account account : this.accountService.readAccounts()) {
            logger.info("account " + account.toString());
        }
        logger.info("---------------------------------------------------------------");
    }

    @Override
    public void setBeanName(String name) {
        this.prefix = name;
    }
}

interface AccountRepository extends JpaRepository<Account, Long> {
}

interface AccountService {
    void deleteAllAccounts();

    List<Account> readAccounts();

    Account readAccount(long id);

    Account createAccount(String username);

    Account createAccountAndNotify(String username);
}

@Service
class JpaAccountService implements AccountService {

    private final JmsTemplate jmsTemplate;

    private final AccountRepository accountRepository;

    @Autowired
    public JpaAccountService(JmsTemplate jmsTemplate,
                             AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.jmsTemplate = jmsTemplate;
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
    public Account createAccount(String username) {
        return this.accountRepository.save(new Account(username));
    }

    @Override
    public Account createAccountAndNotify(String username) {
        Account account = this.createAccount(username);
        this.jmsTemplate.convertAndSend("accounts", account.toString());
        return account;
    }

    @Transactional(readOnly = true)
    public List<Account> readAccounts() {
        return this.accountRepository.findAll();
    }
}

@Service
class JdbcAccountService implements AccountService {

    private final JdbcTemplate jdbcTemplate;

    private final JmsTemplate jmsTemplate;

    private final RowMapper<Account> accountRowMapper = new RowMapper<Account>() {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Account(rs.getLong("id"), rs.getString("username"));
        }
    };

    @Autowired
    public JdbcAccountService(JmsTemplate jmsTemplate, JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jmsTemplate = jmsTemplate;
    }

    @Transactional
    public void deleteAllAccounts() {
        this.jdbcTemplate.update("delete from account");
    }

    @Transactional(readOnly = true)
    public List<Account> readAccounts() {
        return jdbcTemplate.query("select * from account", this.accountRowMapper);
    }

    @Transactional(readOnly = true)
    public Account readAccount(long id) {
        return this.jdbcTemplate.queryForObject("select * from account where id = ?", this.accountRowMapper, (Object) id);
    }

    @Transactional
    public Account createAccountAndNotify(String u) {
        Account account = this.createAccount(u);
        this.jmsTemplate.convertAndSend("accounts", account.toString());
        return account;
    }

    @Transactional
    public Account createAccount(String username) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        PreparedStatementCreatorFactory stmtFactory = new PreparedStatementCreatorFactory(
                "insert into account(id, username) values (nextval('hibernate_sequence'), ?)", new int[]{Types.VARCHAR});
        stmtFactory.setGeneratedKeysColumnNames(new String[]{"id"});
        PreparedStatementCreator psc = stmtFactory.newPreparedStatementCreator(Arrays.asList(username));
        jdbcTemplate.update(psc, keyHolder);
        Number newAccountId = keyHolder.getKey();
        return this.readAccount(newAccountId.longValue());
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

