package org.springframework.boot.autoconfigure.jta;

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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

    private final static AtomicReference<JtaTransactionManager> JTA_TRANSACTION_MANAGER =
            new AtomicReference<JtaTransactionManager>();


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

    public static interface JtaTransactionManagerConfigurer {
        void configureJtaTransactionManager(JtaTransactionManager jtaTransactionManager);
    }

    @Bean
    public JtaTransactionManagerConfigurer jtaTransactionManagerConfigurer() {
        return new JtaTransactionManagerConfigurer() {
            @Autowired(required = false)
            private JmsProperties jmsProperties;

            @Override
            public void configureJtaTransactionManager(JtaTransactionManager jtaTransactionManager) {
                if (null != this.jmsProperties) {
                    this.jmsProperties.setSessionTransacted(true);
                }

            }
        };
    }

    public static final String USER_TRANSACTION_NAME = "jtaUserTransaction";

    public static final String TRANSACTION_MANAGER_NAME = "jtaTransactionManager";

    @java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD,
            java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.PARAMETER,
            java.lang.annotation.ElementType.TYPE,
            java.lang.annotation.ElementType.ANNOTATION_TYPE})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Inherited
    @java.lang.annotation.Documented
    @Qualifier(USER_TRANSACTION_NAME)
    public static @interface UserTransactionBean {
        java.lang.String value() default "";
    }

    @java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD,
            java.lang.annotation.ElementType.METHOD,
            java.lang.annotation.ElementType.PARAMETER,
            java.lang.annotation.ElementType.TYPE,
            java.lang.annotation.ElementType.ANNOTATION_TYPE})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Inherited
    @java.lang.annotation.Documented
    @Qualifier(TRANSACTION_MANAGER_NAME)
    public static @interface TransactionManagerBean {
        java.lang.String value() default "";
    }


    @Bean(name = "transactionManager")
    @ConditionalOnMissingBean(value = PlatformTransactionManager.class)
    @DependsOn({USER_TRANSACTION_NAME, TRANSACTION_MANAGER_NAME})
    public JtaTransactionManager transactionManager(JtaTransactionManagerConfigurer[] jtaTransactionManagerConfigurers,
                                                    @UserTransactionBean UserTransactionImp userTransaction,
                                                    @TransactionManagerBean UserTransactionManager transactionManager) {

        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(
                userTransaction, transactionManager);
        jtaTransactionManager.setAllowCustomIsolationLevels(true);
        jtaTransactionManager.setFailEarlyOnGlobalRollbackOnly(true);
        jtaTransactionManager.setRollbackOnCommitFailure(true);

        JTA_TRANSACTION_MANAGER.set(jtaTransactionManager);

        for (JtaTransactionManagerConfigurer c : jtaTransactionManagerConfigurers)
            c.configureJtaTransactionManager(jtaTransactionManager);

        return jtaTransactionManager;
    }

    @Configuration
    @ConditionalOnClass({com.atomikos.icatch.jta.UserTransactionManager.class})
    public static class AtomikosAutoConfiguration {


        @Bean(initMethod = "init", destroyMethod = "shutdownForce")
        @ConditionalOnMissingBean
        public UserTransactionService userTransactionService(ConfigurableEnvironment e) {

            // setup root data directory
            String path = e.getProperty("spring.jta.atomikos.rootPath",
                    new File(System.getProperty("user.home"), "atomikosData").getAbsolutePath());

            String logBaseDirProperty = "com.atomikos.icatch.log_base_dir";
            String outputDirProperty = "com.atomikos.icatch.output_dir";

            Map<String, Object> rootDataDirProperties = new HashMap<String, Object>();
            rootDataDirProperties.put(outputDirProperty, path);
            rootDataDirProperties.put(logBaseDirProperty, path);

            addEnvironmentProperties(e, rootDataDirProperties);

            // take out any well known properties from the environment and pass to Atomikos
            List<String> wellKnownAtomikosSystemProperties = Arrays.asList(
                    "com.atomikos.icatch.automatic_resource_registration",
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

        private int txTimeout = 10 * 1000;


        @Bean(name = TRANSACTION_MANAGER_NAME, initMethod = "init", destroyMethod = "close")
        @ConditionalOnMissingBean
        @TransactionManagerBean
        public UserTransactionManager atomikosTransactionManager() throws SystemException {
            UserTransactionManager userTransactionManager = new UserTransactionManager();
            userTransactionManager.setForceShutdown(true);
            userTransactionManager.setTransactionTimeout(this.txTimeout);
            return userTransactionManager;
        }

        @Bean(name = USER_TRANSACTION_NAME)
        @ConditionalOnMissingBean
        @UserTransactionBean
        public UserTransactionImp atomikosUserTransaction() throws SystemException {
            UserTransactionImp uti = new UserTransactionImp();
            uti.setTransactionTimeout(this.txTimeout);
            return uti;
        }

        @Bean
        public JtaTransactionManagerConfigurer jpaConfiguration(
                final JpaProperties properties) {

            return new JtaTransactionManagerConfigurer() {

                @Override
                public void configureJtaTransactionManager(JtaTransactionManager jtaTransactionManager) {
                    properties.getProperties().put("hibernate.transaction.jta.platform", AtomikosJtaPlatform.class.getName());
                    properties.getProperties().put("javax.persistence.transactionType", "JTA");
                }
            };
        }

        public static class AtomikosJtaPlatform extends AbstractJtaPlatform {

            private static final long serialVersionUID = 1L;

            @Override
            protected TransactionManager locateTransactionManager() {
                return JTA_TRANSACTION_MANAGER.get().getTransactionManager();
            }

            @Override
            protected UserTransaction locateUserTransaction() {
                return JTA_TRANSACTION_MANAGER.get().getUserTransaction();
            }
        }
    }

    protected static void addEnvironmentProperties(ConfigurableEnvironment environment, Map<String, Object> props) {
        environment.getPropertySources().addFirst(new MapPropertySource("jta", props));
    }

}