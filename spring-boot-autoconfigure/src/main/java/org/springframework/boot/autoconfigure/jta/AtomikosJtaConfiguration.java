package org.springframework.boot.autoconfigure.jta;

import java.util.Properties;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jta.XAConnectionFactoryWrapper;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.boot.jta.atomikos.AtomikosDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.jta.atomikos.AtomikosProperties;
import org.springframework.boot.jta.atomikos.AtomikosXAConnectionFactoryWrapper;
import org.springframework.boot.jta.atomikos.AtomikosXADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;

/**
 * JTA Configuration for <A href="http://www.atomikos.com/">Atomikos</a>.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass(UserTransactionManager.class)
@ConditionalOnMissingBean(PlatformTransactionManager.class)
class AtomikosJtaConfiguration {

	public static final String USER_TRANSACTION_SERVICE = "atomikosUserTransactionService";

	@Bean
	@ConditionalOnMissingBean
	@ConfigurationProperties(prefix = JtaProperties.PREFIX)
	public AtomikosProperties atomikosProperties() {
		return new AtomikosProperties();
	}

	@Bean(initMethod = "init", destroyMethod = "shutdownForce")
	@ConditionalOnMissingBean
	public UserTransactionService userTransactionService(
			AtomikosProperties atomikosProperties) {
		Properties properties = new Properties();
		// FIXME setup files locations
		properties.putAll(atomikosProperties.asProperties());
		return new UserTransactionServiceImp(properties);
	}

	@ConditionalOnMissingBean
	@Bean(initMethod = "init", destroyMethod = "close")
	public UserTransactionManager atomikosTransactionManager(
			UserTransactionService userTransactionService) throws Exception {
		UserTransactionManager manager = new UserTransactionManager();
		manager.setStartupTransactionService(false);
		manager.setForceShutdown(true);
		return manager;
	}

	@Bean
	@ConditionalOnMissingBean
	public UserTransaction atomikosUserTransaction(
			UserTransactionService userTransactionService) {
		return new UserTransactionImp();
	}

	@Bean
	@ConditionalOnMissingBean
	public XADataSourceWrapper xaDataSourceWrapper() {
		return new AtomikosXADataSourceWrapper();
	}

	@Bean
	@ConditionalOnMissingBean
	public XAConnectionFactoryWrapper xaConnectionFactoryWrapper() {
		return new AtomikosXAConnectionFactoryWrapper();
	}

	@Bean
	@ConditionalOnMissingBean
	public AtomikosDependsOnBeanFactoryPostProcessor atomikosDependsOnBeanFactoryPostProcessor() {
		return new AtomikosDependsOnBeanFactoryPostProcessor();
	}

	@Bean
	public JtaTransactionManager transactionManager(UserTransaction userTransaction,
			TransactionManager transactionManager) {
		return new JtaTransactionManager(userTransaction, transactionManager);
	}

}
