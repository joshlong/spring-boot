package org.springframework.boot.autoconfigure.jta;

import java.io.File;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Handles concerns common to all the JTA implementations, including registering the
 * {@link org.springframework.transaction.jta.JtaTransactionManager}, installing a
 * {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform} implementation
 * that will delegate to Spring's registered
 * {@link org.springframework.transaction.jta.JtaTransactionManager} to look-up the
 * required {@link javax.transaction.UserTransaction} and
 * {@link javax.transaction.TransactionManager}.
 *
 * @author Josh Long
 */
@Configuration
abstract class AbstractJtaAutoConfiguration {

	/**
	 * Well-known name for Spring's
	 * {@link org.springframework.transaction.PlatformTransactionManager}
	 */
	public static final String TRANSACTION_MANAGER_NAME = "transactionManager";

	/**
	 * registers Spring's
	 * {@link org.springframework.transaction.jta.JtaTransactionManager}
	 */
	@Primary
	@ConditionalOnMissingBean(name = TRANSACTION_MANAGER_NAME, value = {
			SpringJtaPlatform.class, PlatformTransactionManager.class })
	@Bean(name = TRANSACTION_MANAGER_NAME)
	public JtaTransactionManager transactionManagerBean() throws Exception {
		JtaTransactionManager jtaTransactionManager = buildJtaTransactionManager();
		this.configureJtaTransactionManager(jtaTransactionManager);
		return jtaTransactionManager;
	}

	public String jtaRootPathFor(ConfigurableEnvironment e, String jtaDistribution) {
		return e.getProperty("spring.jta." + jtaDistribution + ".rootPath",
				new File(System.getProperty("user.home"), "jta/" + jtaDistribution
						+ "Data").getAbsolutePath());
	}

	/**
	 * registers Spring's
	 * {@link org.springframework.transaction.jta.JtaTransactionManager}
	 */
	@Primary
	@ConditionalOnMissingBean(name = TRANSACTION_MANAGER_NAME, value = PlatformTransactionManager.class)
	@ConditionalOnBean(SpringJtaPlatform.class)
	@ConditionalOnClass(JtaPlatform.class)
	@Bean(name = TRANSACTION_MANAGER_NAME)
	public JtaTransactionManager transactionManagerBean(SpringJtaPlatform jtaPlatform)
			throws Exception {
		JtaTransactionManager jtaTransactionManager = buildJtaTransactionManager();
		this.configureJtaTransactionManager(jtaTransactionManager);
		return jtaTransactionManager;
	}

	/**
	 * callback for client configuration of the transaction manager
	 */
	protected void configureJtaTransactionManager(
			JtaTransactionManager jtaTransactionManager) {
		// noop for now
	}

	/**
	 * template method for subclasses to contribute their own
	 * {@link org.springframework.transaction.jta.JtaTransactionManager}.
	 */
	protected abstract JtaTransactionManager buildJtaTransactionManager()
			throws Exception;

}
