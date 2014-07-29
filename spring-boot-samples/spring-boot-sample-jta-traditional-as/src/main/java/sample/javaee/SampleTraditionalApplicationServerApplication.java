package sample.javaee;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Demonstrates usage in a Java EE application server like Wildfly
 *
 * @author Josh Long
 */
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleTraditionalApplicationServerApplication
        extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SampleTraditionalApplicationServerApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SampleTraditionalApplicationServerApplication.class, args);
    }

    /**
     * So, the idea behind JNDI is that a server can maintain shared resources like a connection pool and then
     * expose those through a `javax.jms.ConnectionFactory` or a `javax.sql.DataSource` handle that's
     * resolved using a JNDI name, like `java:jboss/datasources/CrmXADS`. This means that configuration lives
     * with the application server, independant of the application. See {@code README.md} file for Wildfly-specific configuration
     * to configure JNDI for a PostgreSQL XA DataSource and a HornetQ JMS message broker.
     *
     * @see <a href="http://planet.jboss.org/post/how_to_create_and_manage_datasources_in_as7">a fairly good
     * introduction for how to configure an XA {@link javax.sql.DataSource} using the <code>jboss-cli.sh</code> tool in the AS' <code>bin</code> directory.</a>
     * @see <a href="https://docs.jboss.org/author/display/WFLY8/JNDI+Reference">
     * https://docs.jboss.org/author/display/WFLY8/JNDI+Reference</a>
     */
    @Bean
    public DataSource dataSource() throws NamingException {
        return InitialContext.doLookup("java:jboss/datasources/CrmXADS");
    }

    /**
     * We can use the default configured JMS connection factory pointing to HornetQ, but
     * make sure to configure the required {@link javax.jms.Destination destination}
     * instances. See {@code README.md} file for Wildfly-specific configuration to configure
     * JNDI for a HornetQ XA DataSource and a PostgresSQL XA DataSource.
     */
    @Bean
    public ConnectionFactory connectionFactory() throws NamingException {
        return InitialContext.doLookup("java:/JmsXA");
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
                accountService.createAccountAndNotify(
                        AccountServiceCommandLineRunner.this.prefix + "-jms");
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


interface AccountService {
    void deleteAllAccounts();

    List<Account> readAccounts();

    Account readAccount(long id);

    Account createAccount(String username);

    Account createAccountAndNotify(String username);
}

@RestController
class AccountRestController {

    @RequestMapping("/accounts")
    Collection<Account> accountCollection() {
        return this.accountService.readAccounts();
    }

    @Autowired
    private AccountService accountService;
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
    public JdbcAccountService(JmsTemplate jmsTemplate,
                              JdbcTemplate jdbcTemplate) {
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

class Account {

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

