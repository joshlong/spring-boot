package sample.narayana;

import java.util.List;

import javax.jms.ConnectionFactory;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.jms.pool.XaPooledConnectionFactory;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jta.arjuna.ArjunaXaDataSourceFactoryBean;
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

/**
 * Arjuna is the name of the JTA sub-project in the larger <A
 * href="http://narayana.jboss.org/">Narayana</A> project which aims to support all things
 * transactional. It can be quite confusing to figure out what libraries to use to make
 * this work in a standalone fashion. The
 * {@code org.springframework.boot:spring-boot-starter-jta-arjuna} starter is invaluable
 * here.
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleArjunaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleArjunaApplication.class, args);
	}

	/**
	 * Registers a {@link javax.sql.DataSource} using the
	 * {@link org.springframework.boot.autoconfigure.jta.arjuna.ArjunaXaDataSourceFactoryBean}
	 * that in turn adapts Arjuna's XA-aware {@link java.sql.Driver} into a
	 * {@link javax.sql.DataSource}.
	 */
	@Bean
	public FactoryBean<DataSource> dataSource() {
		XADataSource xaDataSource = targetDataSource("127.0.0.1", "crm", "crm", "crm");
		return new ArjunaXaDataSourceFactoryBean(xaDataSource, "crm", "crm");
	}

	private XADataSource targetDataSource(String host, String db, String username,
			String pw) {
		PGXADataSource pgxaDataSource = new PGXADataSource();
		pgxaDataSource.setServerName(host);
		pgxaDataSource.setDatabaseName(db);
		pgxaDataSource.setUser(username);
		pgxaDataSource.setPassword(pw);
		return pgxaDataSource;
	}

	/**
	 * Use any XA-aware {@link javax.jms.ConnectionFactory} you want. Here, we're using
	 * the convenient XA-aware connection factory (
	 * {@link org.apache.activemq.jms.pool.XaPooledConnectionFactory
	 * XaPooledConnectionFactory}) from the <A
	 * href="http://activemq.apache.org/">ActiveMQ</A> project itself.
	 * <p>
	 * This also awkwardly requires a reference to the JTA
	 * {@link javax.transaction.TransactionManager}. Here we use the injected
	 * {@link org.springframework.transaction.jta.JtaTransactionManager} to obtain it,
	 * though we could have as easily called a JTA-API specific factory method like
	 * {@link com.arjuna.ats.jta.TransactionManager#transactionManager()}.
	 */
	@Bean
	public ConnectionFactory connectionFactory(JtaTransactionManager transactionManager) {
		ConnectionFactory connectionFactory = targetConnectionFactory("tcp://localhost:61616");
		XaPooledConnectionFactory xa = new XaPooledConnectionFactory();
		xa.setTransactionManager(transactionManager.getTransactionManager());
		xa.setConnectionFactory(connectionFactory);
		return xa;
	}

	private ConnectionFactory targetConnectionFactory(String url) {
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
