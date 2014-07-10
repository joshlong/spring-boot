package org.springframework.boot.autoconfigure.jta;
/*
import com.atomikos.icatch.admin.LogAdministrator;
import com.atomikos.icatch.standalone.UserTransactionServiceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
*/

import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
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
@AutoConfigureBefore({DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@Conditional(JtaAutoConfiguration.JtaCondition.class)
@ConditionalOnClass(JtaTransactionManager.class)
public class JtaAutoConfiguration {


    private final static AtomicReference<JtaTransactionManager> JTA_TRANSACTION_MANAGER =
            new AtomicReference<JtaTransactionManager>();


    public static final Logger logger = LoggerFactory.getLogger(JtaAutoConfiguration.class);

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

    /**
     * Are we running inside a Java EE environment where we want to look up
     * the important bits through a well known JNDI context? Try it and see if it works!
     */
    public static class JavaEeEnvironmentJtaCondition extends SpringBootCondition {
        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            try {
                JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
                jtaTransactionManager.setAutodetectTransactionManager(true);
                jtaTransactionManager.setAutodetectUserTransaction(true);
                jtaTransactionManager.afterPropertiesSet();
                // made it this far, so the setup should've succeeded.
                return ConditionOutcome.match();
            } catch (IllegalStateException e) {
                return ConditionOutcome.noMatch(String.format("couldn't initialize a %s correctly: %s",
                        JtaTransactionManager.class.getName(), e.getMessage()));
            }
        }
    }

    @Autowired(required = false)
    private TransactionManager[] transactionManagers;

    @Autowired(required = false)
    private UserTransaction[] userTransactions;

    @Bean(name = "transactionManager")
    @ConditionalOnMissingBean(value = PlatformTransactionManager.class)
    public JtaTransactionManager transactionManager() {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();

        if (this.userTransactions.length > 0)
            jtaTransactionManager.setUserTransaction(this.userTransactions[0]);

        if (this.transactionManagers.length > 0)
            jtaTransactionManager.setTransactionManager(this.transactionManagers[0]);

        jtaTransactionManager.setAllowCustomIsolationLevels(true);

        JTA_TRANSACTION_MANAGER.set(jtaTransactionManager);
        return jtaTransactionManager;
    }

    @Configuration
    @ConditionalOnClass({com.atomikos.icatch.jta.UserTransactionManager.class })
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

        @Bean(initMethod = "init", destroyMethod = "close")
        @ConditionalOnMissingBean
        public TransactionManager atomikosTransactionManager() throws SystemException {
            return new UserTransactionManager();
        }

        @Bean
        @ConditionalOnMissingBean
        public UserTransaction atomikosUserTransaction() throws SystemException {
            UserTransactionImp uti = new UserTransactionImp();
            uti.setTransactionTimeout(10000);
            return uti;
        }

        @Configuration
        @AutoConfigureAfter(DataSourceAutoConfiguration.class)
        @ConditionalOnClass(JpaVendorAdapter.class)
        @Conditional(HibernateJpaAutoConfiguration.HibernateEntityManagerCondition.class)
        public static class AtomikosHibernateConfiguration implements BeanFactoryAware {

            private BeanFactory beanFactory;

            private String[] getPackagesToScan() {
                if (AutoConfigurationPackages.has(this.beanFactory)) {
                    List<String> basePackages = AutoConfigurationPackages.get(this.beanFactory);
                    return basePackages.toArray(new String[basePackages.size()]);
                }
                return new String[0];
            }

            private Map<String, String> getVendorProperties(DataSource dataSource, JpaProperties jpaProperties) {
                Map<String, String> vendorProperties = jpaProperties.getHibernateProperties(dataSource);
                vendorProperties.put("hibernate.transaction.jta.platform", AtomikosJtaPlatform.class.getName());
                vendorProperties.put("javax.persistence.transactionType", "JTA");
                return vendorProperties;
            }

            @Bean
            @Primary
            @ConditionalOnMissingBean
            public LocalContainerEntityManagerFactoryBean entityManagerFactory(
                    DataSource dataSource, JpaProperties jpaProperties, EntityManagerFactoryBuilder factory) {
                Map<String, String> vals = getVendorProperties(dataSource, jpaProperties);
                return factory.dataSource(dataSource).packages(getPackagesToScan()).properties(vals).build();
            }

            @Override
            public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
                this.beanFactory = beanFactory;
            }


            /**
             * Implements the Hibernate {@link org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform }
             * hook that tells Hibernate where it can find valid references to a JTA {@link javax.transaction.TransactionManager}
             * and a JTA {@link javax.transaction.UserTransaction}. This interface sucks. It's
             * configured as a property that accepts a class name. There doesn't appear to be anyway to
             * provide a pre-configured (for example, from dependency injection) reference. So, we resort to global
             * tricks like this atomic reference.
             */
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
    }

    protected static void addEnvironmentProperties(ConfigurableEnvironment environment, Map<String, Object> props) {
        environment.getPropertySources().addFirst(new MapPropertySource("jta", props));
    }

}



/*

    private static Log logger = LogFactory.getLog(JtaAutoConfiguration.class);

    public static void addEnvironmentProperties(ConfigurableEnvironment environment, Map<String, Object> props) {
        environment.getPropertySources().addFirst(new MapPropertySource("jta", props));
    }

    // configure the third party Atomikos JTA support
    @Configuration
    @ConditionalOnClass(com.atomikos.icatch.jta.UserTransactionImp.class)
    public static class AtomikosJtaAutoConfiguration {

        @Autowired(required = false)
        private LogAdministrator[] logAdministrators;

        @Configuration
        @ConditionalOnClass({com.atomikos.icatch.jta.hibernate3.TransactionManagerLookup.class, LocalContainerEntityManagerFactoryBean.class, EnableTransactionManagement.class, EntityManager.class})
        @Conditional(HibernateJpaAutoConfiguration.HibernateEntityManagerCondition.class)
        @AutoConfigureAfter(DataSourceAutoConfiguration.class)
        @AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
        public static class HibernateAtomikosJtaAutoConfiguration {

            @Autowired
            public void customizeHibernateIfRequired(ConfigurableApplicationContext appContext) {
                Map<String, Object> props = new HashMap<String, Object>();
                props.put("hibernate.transaction.factory_class", com.atomikos.icatch.jta.hibernate3.AtomikosJTATransactionFactory.class.getName());
                props.put("hibernate.transaction.manager_lookup_class", com.atomikos.icatch.jta.hibernate3.TransactionManagerLookup.class.getName());
                addEnvironmentProperties(appContext.getEnvironment(), props);
            }
        }

        @Bean(initMethod = "init", destroyMethod = "shutdownForce")
        @ConditionalOnMissingBean
        public com.atomikos.icatch.config.UserTransactionServiceImp userTransactionService() {
            Properties properties = new Properties();
            properties.setProperty("com.atomikos.icatch", UserTransactionServiceFactory.class.getName());

            com.atomikos.icatch.config.UserTransactionServiceImp userTransactionManager = new com.atomikos.icatch.config.UserTransactionServiceImp(properties);

            if (logAdministrators != null && logAdministrators.length > 0) {
                userTransactionManager.setInitialLogAdministrators(Arrays.asList(logAdministrators));
            }
            return userTransactionManager;
        }

        @Bean(initMethod = "init", destroyMethod = "close")
        @ConditionalOnMissingBean
        public com.atomikos.icatch.jta.UserTransactionManager atomikosUserTransactionManager() throws SystemException {
            com.atomikos.icatch.jta.UserTransactionManager userTransactionManager = new com.atomikos.icatch.jta.UserTransactionManager();
            userTransactionManager.setStartupTransactionService( false );
            userTransactionManager.setForceShutdown(false);
            return userTransactionManager;
        }

        @Bean
        @ConditionalOnMissingBean
        public com.atomikos.icatch.jta.UserTransactionImp atomikosUserTransaction() throws SystemException {
            com.atomikos.icatch.jta.UserTransactionImp userTransactionImp = new com.atomikos.icatch.jta.UserTransactionImp();
            userTransactionImp.setTransactionTimeout(300);

            return userTransactionImp;
        }

        @Bean(name = "transactionManager")
        @ConditionalOnMissingBean(value = PlatformTransactionManager.class)
        public JtaTransactionManager transactionManager(com.atomikos.icatch.jta.UserTransactionManager transactionManager,
                                                        com.atomikos.icatch.jta.UserTransactionImp atomikosUserTransaction) {
            return new JtaTransactionManager(atomikosUserTransaction, transactionManager);
        }
    }

    @Configuration
    @Conditional(JavaEeEnvironmentJtaCondition.class)
    public static class JavaEeJtaAutoConfiguration {

        @Bean(name = "transactionManager")
        @ConditionalOnMissingBean(value = PlatformTransactionManager.class)
        public JtaTransactionManager transactionManager() {
            return new JtaTransactionManager();
        }
    }

    */
/**
 * tests whether the JTA capabilities are part of a full-fledged Java EE environment.
 *//*



    */
/**
 * Tests that we have the requisite types on the CLASSPATH
 *//*


*/

