package sample.javaee;

import java.util.Collection;
import java.util.List;

import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;

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

/**
 * Demonstrates usage in a Java EE application server like Wildfly
 *
 * @author Josh Long
 */
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SampleTraditionalApplicationServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleTraditionalApplicationServerApplication.class, args);
	}

	/**
	 * So, the idea behind JNDI is that a server can maintain shared resources like a
	 * connection pool and then expose those through a `javax.jms.ConnectionFactory` or a
	 * `javax.sql.DataSource` handle that's resolved using a JNDI name, like
	 * `java:jboss/datasources/CrmXADS`. This means that configuration lives with the
	 * application server, independant of the application. See {@code README.md} file for
	 * Wildfly-specific configuration to configure JNDI for a PostgreSQL XA DataSource and
	 * a HornetQ JMS message broker.
	 *
	 * @see <a
	 * href="http://planet.jboss.org/post/how_to_create_and_manage_datasources_in_as7">a
	 * fairly good introduction for how to configure an XA {@link javax.sql.DataSource}
	 * using the <code>jboss-cli.sh</code> tool in the AS' <code>bin</code> directory.</a>
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
	 * instances. See {@code README.md} file for Wildfly-specific configuration to
	 * configure JNDI for a HornetQ XA DataSource and a PostgresSQL XA DataSource.
	 */
	@Bean
	public ConnectionFactory connectionFactory() throws NamingException {
		return InitialContext.doLookup("java:/JmsXA");
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
