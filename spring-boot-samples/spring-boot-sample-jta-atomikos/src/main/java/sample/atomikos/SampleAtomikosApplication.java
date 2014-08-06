package sample.atomikos;

import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import sample.atomikos.service.AccountService;

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
