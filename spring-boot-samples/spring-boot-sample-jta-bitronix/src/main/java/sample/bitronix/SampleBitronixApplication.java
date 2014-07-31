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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Collection;
import java.util.List;

/**
 * This sample uses the <A href="http://docs.codehaus.org/display/BTM/Home">Bitronix JTA</A>
 * engine. Bitronix is a single Maven dependency. You can use the
 * {@code org.springframework.boot:spring-boot-starter-jta-bitronix} starter or simply import
 * the dependency itself. This code works against the very stable Bitronix 2.1 line.
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

    /**
     * Bitronix offers a {@link bitronix.tm.resource.jms.PoolingConnectionFactory}
     * that - for reasons as yet not understood - does not accept a valid {@link javax.jms.XAConnectionFactory}
     * directly, but instead expects to instantiate and initializing a target itself.
     */
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

    /**
     * Bitronix offers a {@link bitronix.tm.resource.jdbc.PoolingDataSource}
     * that - for reasons as yet not understood - does not accept a valid {@link javax.sql.XADataSource}
     * directly, but instead expects to instantiate and initializing a target itself.
     */
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
    public CommandLineRunner jpa(AccountService accountService) {
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


@RestController
class AccountRestController {

    @RequestMapping("/accounts")
    Collection<Account> accountCollection() {
        return this.accountService.readAccounts();
    }

    @Autowired
    private AccountService accountService;
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

