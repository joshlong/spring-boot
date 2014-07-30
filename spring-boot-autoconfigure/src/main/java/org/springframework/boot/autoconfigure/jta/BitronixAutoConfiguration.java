package org.springframework.boot.autoconfigure.jta;

import java.io.File;

import javax.transaction.TransactionManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import bitronix.tm.TransactionManagerServices;

/**
 * Registers the <A href="http://docs.codehaus.org/display/BTM/Home">Bitronix JTA </A>
 * implementation and configures JTA support. Requires that clients register their
 * {@link javax.sql.DataSource}s with {@link bitronix.tm.resource.jdbc.PoolingDataSource}
 * and their JMS {@link javax.jms.ConnectionFactory}s with
 * {@link bitronix.tm.resource.jms.PoolingConnectionFactory}. It can be a little clumsy to
 * configure the Bitronix pooling implementations. The
 * {@link org.springframework.boot.autoconfigure.jta.bitronix.BitronixXaConnectionFactoryFactoryBean}
 * and
 * {@link org.springframework.boot.autoconfigure.jta.bitronix.BitronixXaDataSourceFactoryBean}
 * provide a convenient, type-safe way to configure the Bitronix {@code -Pooling}
 * implementations.
 *
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass(bitronix.tm.jndi.BitronixContext.class)
@ConditionalOnMissingBean(name = "transactionManager", value = PlatformTransactionManager.class)
public class BitronixAutoConfiguration extends AbstractJtaAutoConfiguration {

	private String bitronixPropertyPrefix = "spring.jta.bitronix.";

	@Autowired
	private ConfigurableEnvironment configurableEnvironment;

	@Bean
	@ConditionalOnMissingBean
	public TransactionManager bitronixTransactionManager(
			bitronix.tm.Configuration configuration) {
		configuration.setDisableJmx(true); // todo properties
		return TransactionManagerServices.getTransactionManager();
	}

	@Bean
	@ConditionalOnMissingBean
	public bitronix.tm.Configuration bitronixConfiguration(
			ConfigurableEnvironment environment) {
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

	@Override
	protected JtaTransactionManager buildJtaTransactionManager() throws Exception {
		return new JtaTransactionManager(
				this.bitronixTransactionManager(bitronixConfiguration(this.configurableEnvironment)));
	}
}
