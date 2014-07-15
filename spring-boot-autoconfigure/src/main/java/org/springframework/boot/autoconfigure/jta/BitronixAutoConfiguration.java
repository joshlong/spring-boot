package org.springframework.boot.autoconfigure.jta;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.BitronixTransactionSynchronizationRegistry;
import bitronix.tm.TransactionManagerServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.TransactionManager;

@Configuration
class BitronixAutoConfiguration {

    private String bitronixPropertyPrefix = "spring.jta.bitronix.";

    @Autowired(required = false)
    private JmsProperties jmsProperties;

    @Autowired(required = false)
    private JpaProperties jpaProperties;

    @Primary
    @Bean(name = "transactionManager")
    @ConditionalOnMissingBean(name = "transactionManager")
    public JtaTransactionManager transactionManager(@TransactionManagerBean TransactionManager transactionManager) {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(transactionManager);
        JtaAutoConfiguration.configureJtaProperties(jtaTransactionManager, this.jmsProperties, this.jpaProperties);
        return jtaTransactionManager;
    }

    @TransactionManagerBean(destroyMethod = "shutdown")
    public BitronixTransactionManager jtaTransactionManager(bitronix.tm.Configuration configuration) {
        configuration.setDisableJmx(true);
        return TransactionManagerServices.getTransactionManager();
    }

    @Bean
    public bitronix.tm.Configuration configuration(Environment environment) {
        bitronix.tm.Configuration configuration = TransactionManagerServices.getConfiguration();
        String serverId = environment.getProperty(
                this.bitronixPropertyPrefix + "serverId", "spring-boot-jta-bitronix");
        configuration.setServerId(serverId);
        return configuration;
    }

    @Bean
    public BitronixTransactionSynchronizationRegistry transactionSynchronizationRegistry(bitronix.tm.Configuration configuration) {
        return TransactionManagerServices.getTransactionSynchronizationRegistry();
    }
}
