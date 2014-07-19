package org.springframework.boot.autoconfigure.jta;

import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.TransactionManagerServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.TransactionManager;
import java.io.File;

/**
 * @author Josh Long
 */
@Configuration
class BitronixAutoConfiguration  extends AbstractJtaAutoConfiguration {

    private String bitronixPropertyPrefix = "spring.jta.bitronix.";

    @Bean
    @ConditionalOnMissingBean
    public TransactionManager bitronixTransactionManager(
            bitronix.tm.Configuration configuration) {
        configuration.setDisableJmx(true);
        return TransactionManagerServices.getTransactionManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public bitronix.tm.Configuration bitronixConfiguration(
            ConfigurableEnvironment environment) {
        bitronix.tm.Configuration configuration = TransactionManagerServices.getConfiguration();
        String serverId = environment.getProperty(
                this.bitronixPropertyPrefix + "serverId", "spring-boot-jta-bitronix");
        File rootPath = new File(JtaAutoConfiguration.jtaRootPathFor(environment, "bitronix"));
        configuration.setServerId(serverId);
        configuration.setLogPart1Filename(new File(rootPath, "btm1").getAbsolutePath());
        configuration.setLogPart2Filename(new File(rootPath, "btm2").getAbsolutePath());
        return configuration;
    }

    @Bean
    @ConditionalOnMissingBean
    public BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry(bitronix.tm.Configuration configuration) {
        return TransactionManagerServices.getTransactionSynchronizationRegistry();
    }

    @Override
    protected JtaTransactionManager buildJtaTransactionManager() throws Exception {
        return new JtaTransactionManager(this.bitronixTransactionManager(bitronixConfiguration(this.configurableEnvironment)));
    }

    @Autowired
    private ConfigurableEnvironment configurableEnvironment;
}
