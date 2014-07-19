package org.springframework.boot.autoconfigure.jta;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Handles concerns common to all the JTA implementations, including registering the {@link org.springframework.transaction.jta.JtaTransactionManager},
 * installing a {@link org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform} implementation that will delegate
 * to Spring's registered {@link org.springframework.transaction.jta.JtaTransactionManager} to look-up
 * the required {@link javax.transaction.UserTransaction} and {@link javax.transaction.TransactionManager}.
 *
 * @author Josh Long
 */
@Configuration
abstract class AbstractJtaAutoConfiguration {

    /**
     * Well-known name for Spring's {@link org.springframework.transaction.PlatformTransactionManager}
     */
    public static final String TRANSACTION_MANAGER_NAME = "transactionManager";

    @Autowired(required = false)
    private JmsProperties jmsProperties;

    @Autowired(required = false)
    private JpaProperties jpaProperties;

    @Bean
    @ConditionalOnClass(org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform.class)
    @ConditionalOnMissingBean(SpringJtaPlatform.class)
    public SpringJtaPlatform jtaPlatform() {
        return new SpringJtaPlatform();
    }

    /**
     * registers Spring's {@link org.springframework.transaction.jta.JtaTransactionManager}
     */
    @Primary
    @ConditionalOnMissingBean(name = TRANSACTION_MANAGER_NAME,
            value = {SpringJtaPlatform.class, PlatformTransactionManager.class})
    @Bean(name = TRANSACTION_MANAGER_NAME)
    public JtaTransactionManager transactionManagerBean() throws Exception {

        JtaTransactionManager jtaTransactionManager = buildJtaTransactionManager();

        this.configureJtaTransactionManager(jtaTransactionManager);
        this.configureJmsProperties(jmsProperties);
        this.configureJpaProperties(jpaProperties);

        return jtaTransactionManager;
    }


    /**
     * registers Spring's {@link org.springframework.transaction.jta.JtaTransactionManager}
     */
    @Primary
    @ConditionalOnMissingBean(name = TRANSACTION_MANAGER_NAME,
            value = PlatformTransactionManager.class)
    @ConditionalOnBean(SpringJtaPlatform.class)
    @Bean(name = TRANSACTION_MANAGER_NAME)
    public JtaTransactionManager transactionManagerBean(SpringJtaPlatform jtaPlatform) throws Exception {

        JtaTransactionManager jtaTransactionManager = buildJtaTransactionManager();

        // make this available for JPA integration with JTA, if required.
        jtaPlatform.JTA_TRANSACTION_MANAGER.set(jtaTransactionManager);

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
