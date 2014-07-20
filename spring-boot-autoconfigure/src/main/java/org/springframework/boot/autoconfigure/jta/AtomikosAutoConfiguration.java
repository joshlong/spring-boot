package org.springframework.boot.autoconfigure.jta;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import java.util.*;

/**
 *
 * Registers the <A href="http://www.atomikos.com/">Atomikos JTA</a> implementation and
 * configures JTA support. Requires that clients register their {@link javax.sql.DataSource}s with
 * {@link com.atomikos.jdbc.AtomikosDataSourceBean} and their
 * JMS {@link javax.jms.ConnectionFactory}s with
 * {@link com.atomikos.jms.AtomikosConnectionFactoryBean}.
 *
 * @author Josh Long
 */
@Configuration
class AtomikosAutoConfiguration extends AbstractJtaAutoConfiguration {

    public  static final String USER_TRANSACTION_SERVICE = "atomikosUserTransactionService";


    @ConditionalOnMissingBean
    @Bean(name = USER_TRANSACTION_SERVICE, initMethod = "init", destroyMethod = "shutdownForce")
    public UserTransactionService userTransactionService(ConfigurableEnvironment e) {

        // setup root data directory
        String atomikos = "atomikos";
        String path = JtaAutoConfiguration.jtaRootPathFor(e, atomikos);

        String logBaseDirProperty = "com.atomikos.icatch.log_base_dir";
        String outputDirProperty = "com.atomikos.icatch.output_dir";
        String autoEnroll = "com.atomikos.icatch.automatic_resource_registration";

        Map<String, Object> rootDataDirProperties = new HashMap<String, Object>();
        rootDataDirProperties.put(outputDirProperty, path);
        rootDataDirProperties.put(logBaseDirProperty, path);

        rootDataDirProperties.put("com.atomikos.icatch.threaded_2pc", "false");
        rootDataDirProperties.put(autoEnroll, "false");

        e.getPropertySources().addFirst(
                new MapPropertySource(atomikos, rootDataDirProperties));

        // take out any well known properties from the environment and pass to Atomikos
        List<String> wellKnownAtomikosSystemProperties = Arrays.asList(
                autoEnroll,
                "com.atomikos.icatch.client_demarcation",
                "com.atomikos.icatch.threaded_2pc",
                "com.atomikos.icatch.serial_jta_transactions",
                "com.atomikos.icatch.serializable_logging",
                "com.atomikos.icatch.max_actives",
                "com.atomikos.icatch.checkpoint_interval",
                "com.atomikos.icatch.enable_logging",
                logBaseDirProperty,
                outputDirProperty,
                "com.atomikos.icatch.log_base_name",
                "com.atomikos.icatch.max_timeout",
                "com.atomikos.icatch.tm_unique_name",
                "java.naming.factory.initial",
                "java.naming.provider.url",
                "com.atomikos.icatch.service",
                "com.atomikos.icatch.force_shutdown_on_vm_exit");

        Properties properties = new Properties();
        for (String k : wellKnownAtomikosSystemProperties) {
            if (e.containsProperty(k)) {
                properties.setProperty(k, e.getProperty(k));
            }
        }
        return new UserTransactionServiceImp(properties);
    }


    @ConditionalOnMissingBean
    @DependsOn(USER_TRANSACTION_SERVICE)
    @Bean(initMethod = "init", destroyMethod = "close")
    public UserTransactionManager atomikosTransactionManager() throws SystemException {
        UserTransactionManager userTransactionManager = new UserTransactionManager();
        userTransactionManager.setForceShutdown(true);
        userTransactionManager.setTransactionTimeout(10 * 1000);
        return userTransactionManager;
    }

    @Override
    protected JtaTransactionManager buildJtaTransactionManager() throws Exception {
        return new JtaTransactionManager((TransactionManager) this.atomikosTransactionManager());
    }
}
