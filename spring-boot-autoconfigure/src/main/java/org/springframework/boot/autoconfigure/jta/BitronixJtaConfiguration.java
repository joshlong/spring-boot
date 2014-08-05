package org.springframework.boot.autoconfigure.jta;

import java.io.File;

import javax.transaction.TransactionManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jta.XADataSourceWrapper;
import org.springframework.boot.jta.bitronix.PollingDataSourceXAWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.jndi.BitronixContext;

/**
 * Registers the <A href="http://docs.codehaus.org/display/BTM/Home">Bitronix JTA </A>
 * implementation and configures JTA support
 *
 * @author Josh Long
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnClass(BitronixContext.class)
@ConditionalOnMissingBean(PlatformTransactionManager.class)
class BitronixJtaConfiguration {

	private String bitronixPropertyPrefix = "spring.jta.bitronix.";

	@Autowired
	private ConfigurableEnvironment configurableEnvironment;

	@Bean
	@ConditionalOnMissingBean
	public bitronix.tm.Configuration bitronixConfiguration(Environment environment) {
		bitronix.tm.Configuration configuration = TransactionManagerServices
				.getConfiguration();
		String serverId = environment.getProperty(this.bitronixPropertyPrefix
				+ "serverId", "spring-boot-jta-bitronix");
		File rootPath = new File(this.jtaRootPathFor(environment, "bitronix"));
		configuration.setServerId(serverId);
		configuration.setLogPart1Filename(new File(rootPath, "btm1").getAbsolutePath());
		configuration.setLogPart2Filename(new File(rootPath, "btm2").getAbsolutePath());
		return configuration;
	}

	@Bean
	@ConditionalOnMissingBean
	public TransactionManager bitronixTransactionManager(
			bitronix.tm.Configuration configuration) {
		configuration.setDisableJmx(true); // FIXME properties
		return TransactionManagerServices.getTransactionManager();
	}

	@Bean
	@ConditionalOnMissingBean
	public XADataSourceWrapper xaDataSourceWrapper() {
		return new PollingDataSourceXAWrapper();
	}

	private String jtaRootPathFor(Environment e, String jtaDistribution) {
		// FIXME delete
		return e.getProperty("spring.jta." + jtaDistribution + ".rootPath",
				new File(System.getProperty("user.home"), "jta/" + jtaDistribution
						+ "Data").getAbsolutePath());
	}

	@Bean
	public JtaTransactionManager transactionManager(TransactionManager transactionManager) {
		return new JtaTransactionManager(transactionManager);
	}
}
