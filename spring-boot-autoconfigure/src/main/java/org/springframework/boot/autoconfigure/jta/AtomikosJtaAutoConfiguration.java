package org.springframework.boot.autoconfigure.jta;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

/**
 * JTA Configuration for <A href="http://www.atomikos.com/">Atomikos</a>.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @since 1.2.0
 */
@Configuration
@Conditional(JtaCondition.class)
@ConditionalOnClass(com.atomikos.icatch.jta.UserTransactionManager.class)
class AtomikosJtaAutoConfiguration {

	public static final String USER_TRANSACTION_SERVICE = "atomikosUserTransactionService";

	@ConditionalOnMissingBean
	@Bean(name = USER_TRANSACTION_SERVICE, initMethod = "init", destroyMethod = "shutdownForce")
	public UserTransactionService userTransactionService(ConfigurableEnvironment env) {

		// setup root data directory
		String path = this.jtaRootPathFor(env, "atomikos");

		String logBaseDirProperty = "com.atomikos.icatch.log_base_dir";
		String outputDirProperty = "com.atomikos.icatch.output_dir";
		String autoEnroll = "com.atomikos.icatch.automatic_resource_registration";

		Map<String, Object> rootDataDirProperties = new HashMap<String, Object>();
		rootDataDirProperties.put(outputDirProperty, path);
		rootDataDirProperties.put(logBaseDirProperty, path);

		rootDataDirProperties.put("com.atomikos.icatch.threaded_2pc", "false");
		rootDataDirProperties.put(autoEnroll, "false");

		env.getPropertySources().addFirst(
				new MapPropertySource("atomikos", rootDataDirProperties));

		// take out any well known properties from the environment and pass to Atomikos
		List<String> wellKnownAtomikosSystemProperties = Arrays.asList(autoEnroll,
				"com.atomikos.icatch.client_demarcation",
				"com.atomikos.icatch.threaded_2pc",
				"com.atomikos.icatch.serial_jta_transactions",
				"com.atomikos.icatch.serializable_logging",
				"com.atomikos.icatch.max_actives",
				"com.atomikos.icatch.checkpoint_interval",
				"com.atomikos.icatch.enable_logging", logBaseDirProperty,
				outputDirProperty, "com.atomikos.icatch.log_base_name",
				"com.atomikos.icatch.max_timeout", "com.atomikos.icatch.tm_unique_name",
				"java.naming.factory.initial", "java.naming.provider.url",
				"com.atomikos.icatch.service",
				"com.atomikos.icatch.force_shutdown_on_vm_exit");

		Properties properties = new Properties();
		for (String k : wellKnownAtomikosSystemProperties) {
			if (env.containsProperty(k)) {
				properties.setProperty(k, env.getProperty(k));
			}
		}
		return new UserTransactionServiceImp(properties);
	}

	private String jtaRootPathFor(Environment e, String jtaDistribution) {
		// FIXME delete
		return e.getProperty("spring.jta." + jtaDistribution + ".rootPath",
				new File(System.getProperty("user.home"), "jta/" + jtaDistribution
						+ "Data").getAbsolutePath());
	}

	// TODO The UserTransactionManager must start *after* the pools are initialized and be
	// destroyed *before* the pools are destroyed
	@ConditionalOnMissingBean
	@DependsOn(USER_TRANSACTION_SERVICE)
	@Bean(initMethod = "init", destroyMethod = "close")
	public UserTransactionManager atomikosTransactionManager() throws SystemException {
		UserTransactionManager userTransactionManager = new UserTransactionManager();
		userTransactionManager.setForceShutdown(true);
		userTransactionManager.setTransactionTimeout(10 * 1000);
		return userTransactionManager;
	}

	private JtaTransactionManager buildJtaTransactionManager() throws Exception {
		return new JtaTransactionManager(
				(TransactionManager) atomikosTransactionManager());
	}
}
