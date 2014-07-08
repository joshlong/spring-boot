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
//@Configuration
//@AutoConfigureBefore({DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
//@Conditional(JtaAutoConfiguration.JtaCondition.class)
//@ConditionalOnClass(JtaTransactionManager.class)
public class JtaAutoConfiguration {
/*

    private static Log logger = LogFactory.getLog(JtaAutoConfiguration.class);

    public static void addEnvironmentProperty(ConfigurableEnvironment environment, Map<String, Object> props) {
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
                addEnvironmentProperty(appContext.getEnvironment(), props);
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
        public JtaTransactionManager transactionManager(com.atomikos.icatch.jta.UserTransactionManager atomikosTransactionManager,
                                                        com.atomikos.icatch.jta.UserTransactionImp atomikosUserTransaction) {
            return new JtaTransactionManager(atomikosUserTransaction, atomikosTransactionManager);
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
                return ConditionOutcome.noMatch("couldn't initialize a " +
                        JtaTransactionManager.class.getName() + " correctly.");
            }
        }
    }

    */
/**
     * Tests that we have the requisite types on the CLASSPATH
     *//*

    public static class JtaCondition extends SpringBootCondition {

        private static String[] CLASS_NAMES =
                "javax.jms.XAConnectionFactory,javax.sql.XADataSource".split(",");

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            // basic test for JTA classes
            for (String className : CLASS_NAMES) {
                if (ClassUtils.isPresent(className, context.getClassLoader())) {
                    return ConditionOutcome.match("found an XA class on the CLASSPATH.");
                }
            }
            return ConditionOutcome.noMatch("no XA class.");
        }
    }
*/
}
