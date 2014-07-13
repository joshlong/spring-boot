package org.springframework.boot.autoconfigure.jta;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * Attempts to register JTA transaction managers.
 * <p> Will support several strategies. Thinking aloud, these might include:
 * <p>
 * <OL>
 * <LI> Atomikos & (Tomcat || Jetty) </li>
 * <LI> BTM & (Tomcat || Jetty) </li>
 * <LI> JOTM & (Tomcat || Jetty)</li>
 * <LI> Narayana & (Tomcat || Jetty) </li>
 * <li>Standard Application server JTA search strategy as supported directly
 * by {@link org.springframework.transaction.jta.JtaTransactionManager}.</li>
 * </OL>
 * <p>
 * <p>
 * For a start, Spring Boot will try to pull well-known transactional resources in a
 * a given bean container.
 *
 * @author Josh Long
 */
@Configuration
@AutoConfigureBefore(DataSourceTransactionManagerAutoConfiguration.class)
@Conditional(JtaAutoConfiguration.JtaCondition.class)
@ConditionalOnClass(JtaTransactionManager.class)
public class JtaAutoConfiguration {


    @Bean
    public InitializingBean initializingBean(final JtaTransactionManager jtaTransactionManager) {

        SpringJtaPlatform.JTA_TRANSACTION_MANAGER.set(jtaTransactionManager);

        return new InitializingBean() {
            @Override
            public void afterPropertiesSet() throws Exception {
            }
        };
    }

    /**
     * Are we running in an environment that has basic JTA-capable types on the CLASSPATH
     * <EM>and</EM> that has JTA-infrastructure beans, like a {@link javax.transaction.UserTransaction}
     * and a {@link javax.transaction.TransactionManager}.
     */
    public static class JtaCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ClassLoader classLoader = context.getClassLoader();

            // make sure we have both JTA transaction manager types of interest
            boolean jtaTransactionManagerTypesOnClassPath = ClassUtils.isPresent("javax.transaction.UserTransaction", classLoader) &&
                    ClassUtils.isPresent("javax.transaction.TransactionManager", classLoader);

            // and a type that can be used as a resource (such as an XADataSource or a XAConnectionFactory)
            boolean jtaResourceTypeOnClassPath = ClassUtils.isPresent("javax.jms.XAConnectionFactory", classLoader) ||
                    ClassUtils.isPresent("javax.sql.XADataSource", classLoader);

            if (jtaTransactionManagerTypesOnClassPath && jtaResourceTypeOnClassPath) {
                return ConditionOutcome.match();
            }

            return ConditionOutcome.noMatch("no XA class.");
        }
    }


    public static final String USER_TRANSACTION_NAME = "jtaUserTransaction";

    public static final String TRANSACTION_MANAGER_NAME = "jtaTransactionManager";

    public static void configureJtaProperties(JtaTransactionManager jtaTransactionManager,
                                              JmsProperties jmsProperties,
                                              JpaProperties jpaProperties) {
        if (null != jmsProperties) {
            jmsProperties.setSessionTransacted(true);
        }
        if (jpaProperties != null) {
            jpaProperties.getProperties().put(
                    "hibernate.transaction.jta.platform", SpringJtaPlatform.class.getName());
            jpaProperties.getProperties().put("javax.persistence.transactionType", "JTA");
        }

    }


    @Configuration
    @ConditionalOnClass(com.arjuna.ats.jta.UserTransaction.class)
    @Import(NarayanaAutoConfiguration.class)
    @ConditionalOnMissingBean(name = "transactionManager", value = PlatformTransactionManager.class)
    public static class NarayanaJBossTmJTaConfiguration {
    }

    @Configuration
    @ConditionalOnClass(com.atomikos.icatch.jta.UserTransactionManager.class)
    @Import(AtomikosAutoConfiguration.class)
    @ConditionalOnMissingBean(name = "transactionManager", value = PlatformTransactionManager.class)
    public static class AtomikosJTaConfiguration {
    }

    protected static void addEnvironmentProperties(ConfigurableEnvironment environment, Map<String, Object> props) {
        environment.getPropertySources().addFirst(
                new MapPropertySource("jta", props));
    }

}