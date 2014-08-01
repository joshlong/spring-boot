package sample.atomikos;

import java.util.Collection;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.AtomikosConnectionFactoryBean;

/**
 * <A href="http://www.atomikos.com">Atomikos</A>, the company, sell products and support
 * around transaction management. They offer a very handy open-source package called
 * TransactionEssentials, which is what Spring Boot integrates.
 * <p>
 * This implementation uses the very convenient
 * {@code org.springframework.boot:spring-boot-starter-jta-atomikos} starter to bring in
 * the right types for Atomikos's core JTA support as well as its JDBC (
 * {@code com.atomikos:transactions-jdbc}) and JMS support (
 * {@code com.atomikos:transactions-jms})
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleAtomikosApplication {

	private int poolSize = 10;

	public static void main(String[] args) {
		SpringApplication.run(SampleAtomikosApplication.class, args);
	}

	/**
	 * The Atomikos project provide a wrapper {@link javax.sql.DataSource} implementation.
	 * The {@link com.atomikos.jdbc.AtomikosDataSourceBean} expects a reference to a
	 * {@link javax.sql.XADataSource}.
	 */
	@Bean(initMethod = "init", destroyMethod = "close")
	public AtomikosDataSourceBean dataSource() {
		AtomikosDataSourceBean xa = new AtomikosDataSourceBean();
		xa.setXaDataSource(targetDataSource());
		xa.setUniqueResourceName("dataSource");
		xa.setTestQuery("select now()");
		xa.setPoolSize(this.poolSize);
		return xa;
	}

	private JdbcDataSource targetDataSource() {
		JdbcDataSource xaDataSource = new JdbcDataSource();
		xaDataSource.setPassword("sa");
		xaDataSource.setURL("jdbc:h2:tcp://localhost/~/crm");
		xaDataSource.setUser("sa");
		return xaDataSource;
	}

	/**
	 * The Atomikos project provide a wrapper {@link javax.jms.ConnectionFactory}
	 * implementation. The {@link com.atomikos.jms.AtomikosConnectionFactoryBean} expects
	 * a reference to a {@link javax.jms.XAConnectionFactory}.
	 */
	@Bean(initMethod = "init", destroyMethod = "close")
	public AtomikosConnectionFactoryBean connectionFactory() {
		AtomikosConnectionFactoryBean xa = new AtomikosConnectionFactoryBean();
		xa.setXaConnectionFactory(targetConnectionFactory("tcp://localhost:61616"));
		xa.setUniqueResourceName("connectionFactory");
		xa.setPoolSize(this.poolSize);
		return xa;
	}

	private ActiveMQXAConnectionFactory targetConnectionFactory(String url) {
		return new ActiveMQXAConnectionFactory(url);
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
		this.logger.info(this.prefix);

		this.transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				AccountServiceCommandLineRunner.this.accountService
						.createAccountAndNotify(AccountServiceCommandLineRunner.this.prefix
								+ "-jms");
				iterateAccounts("insert");
				status.setRollbackOnly();
			}
		});
		iterateAccounts("after");
	}

	protected void iterateAccounts(String msg) {
		this.logger
				.info("---------------------------------------------------------------");
		this.logger.info("accounts: " + this.prefix + ": " + msg);
		this.logger
				.info("---------------------------------------------------------------");
		for (Account account : this.accountService.readAccounts()) {
			this.logger.info("account " + account.toString());
		}
		this.logger
				.info("---------------------------------------------------------------");
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
	public JpaAccountService(JmsTemplate jmsTemplate, AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	@Transactional
	public void deleteAllAccounts() {
		this.accountRepository.deleteAllInBatch();
	}

	@Override
	@Transactional(readOnly = true)
	public Account readAccount(long id) {
		return this.accountRepository.findOne(id);
	}

	@Override
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

	@Override
	@Transactional(readOnly = true)
	public List<Account> readAccounts() {
		return this.accountRepository.findAll();
	}
}

@RestController
class AccountRestController {

	@Autowired
	private AccountService accountService;

	@RequestMapping("/accounts")
	Collection<Account> accountCollection() {
		return this.accountService.readAccounts();
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
		sb.append("id=").append(this.id);
		sb.append(", username='").append(this.username).append('\'');
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
		return this.id;
	}

	public String getUsername() {
		return this.username;
	}
}
