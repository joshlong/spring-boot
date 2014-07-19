package org.springframework.boot.autoconfigure.jta;


import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * @author Josh Long
 */
@Configuration
abstract class BaseJtaAutoConfiguration {

    /**
     * Well-known name for Spring's {@link org.springframework.transaction.PlatformTransactionManager}
     */
    public static final String TRANSACTION_MANAGER_NAME = "transactionManager";

    /**
     * registers Spring's {@link org.springframework.transaction.jta.JtaTransactionManager}
     */
    @Primary
    @ConditionalOnMissingBean(name = TRANSACTION_MANAGER_NAME,
            value = PlatformTransactionManager.class)
    @Bean(name = TRANSACTION_MANAGER_NAME)
    public JtaTransactionManager transactionManagerBean(
            JmsProperties jmsProperties,
            JpaProperties jpaProperties) throws Exception {

        JtaTransactionManager jtaTransactionManager = buildJtaTransactionManager();

        // make this available for JPA, if required.
        SpringJtaPlatform.JTA_TRANSACTION_MANAGER.set(jtaTransactionManager);

        this.configureJtaTransactionManager(jtaTransactionManager);
        this.configureJmsProperties(jmsProperties);
        this.configureJpaProperties(jpaProperties);

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
     * callback for contribution to the
     * {@link org.springframework.boot.autoconfigure.orm.jpa.JpaProperties}, if required.
     */
    protected void configureJpaProperties(JpaProperties jpaProperties) {
        if (null != jpaProperties) {
            jpaProperties.getProperties().put(
                    "hibernate.transaction.jta.platform", SpringJtaPlatform.class.getName());
            jpaProperties.getProperties().put("javax.persistence.transactionType", "JTA");
        }
    }

    /**
     * Callback for configuration of {@link org.springframework.boot.autoconfigure.jms.JmsProperties}
     */
    protected void configureJmsProperties(JmsProperties jmsProperties) {
        if (null != jmsProperties) {
            jmsProperties.setSessionTransacted(true);
        }
    }

    /**
     * template method for subclasses to contribute their
     * own {@link org.springframework.transaction.jta.JtaTransactionManager}.
     */
    protected abstract JtaTransactionManager buildJtaTransactionManager() throws Exception;


}
