package org.springframework.boot.autoconfigure.jta;

import com.atomikos.icatch.admin.LogAdministrator;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.jms.XAConnectionFactory;
import javax.sql.XADataSource;
import javax.transaction.SystemException;
import java.util.Arrays;
import java.util.Properties;

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
@ConditionalOnClass({XAConnectionFactory.class, XADataSource.class, JtaTransactionManager.class})
public class JtaTransactionManagerAutoConfiguration {

    public static final String WELL_KNOWN_TRANSACTION_MANAGER_BEAN_NAME = "transactionManager";


    // configure Atomikos
    @Configuration
    @ConditionalOnClass(com.atomikos.icatch.jta.UserTransactionImp.class)
    public static class AtomikosJtaAutoConfiguration {
        /*@Autowired
          void tailorProperties(Environment properties) {
            properties.getP("hibernate.transaction.manager_lookup_class",
                    TransactionManagerLookup.class.getName());
        }*/



        @Bean
        @ConditionalOnMissingBean
        public com.atomikos.icatch.admin.imp.LocalLogAdministrator localLogAdministrator() {
            return new com.atomikos.icatch.admin.imp.LocalLogAdministrator();
        }

        @Bean(destroyMethod = "shutdownForce")
        @ConditionalOnMissingBean
        public com.atomikos.icatch.config.UserTransactionServiceImp userTransactionService(
                LogAdministrator[] logAdministrators) {
            Properties properties = new Properties();
            properties.setProperty("com.atomikos.icatch",
                    "com.atomikos.icatch.standalone.UserTransactionServiceFactory");
            com.atomikos.icatch.config.UserTransactionServiceImp uts =
                    new com.atomikos.icatch.config.UserTransactionServiceImp(properties);
            if (logAdministrators != null && logAdministrators.length > 0)
                uts.setInitialLogAdministrators(Arrays.asList(logAdministrators));

            uts.init();

            return uts;
        }

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean
        public com.atomikos.icatch.jta.UserTransactionManager atomikosTransactionManager() throws SystemException {
            com.atomikos.icatch.jta.UserTransactionManager userTransactionManager
                    = new com.atomikos.icatch.jta.UserTransactionManager();
            userTransactionManager.setForceShutdown(false);
            userTransactionManager.setStartupTransactionService(false);
            userTransactionManager.init();
            return userTransactionManager;
        }

        @Bean
        @ConditionalOnMissingBean
        public com.atomikos.icatch.jta.UserTransactionImp atomikosUserTransaction() throws SystemException {
            com.atomikos.icatch.jta.UserTransactionImp userTransactionImp =
                    new com.atomikos.icatch.jta.UserTransactionImp();
            userTransactionImp.setTransactionTimeout(300);
            return userTransactionImp;
        }

        @Bean(name = WELL_KNOWN_TRANSACTION_MANAGER_BEAN_NAME)
        @ConditionalOnMissingBean(name = WELL_KNOWN_TRANSACTION_MANAGER_BEAN_NAME)
        public JtaTransactionManager jtaTransactionManager(com.atomikos.icatch.jta.UserTransactionManager atomikosTransactionManager,
                                                           com.atomikos.icatch.jta.UserTransactionImp atomikosUserTransaction) {
            return new JtaTransactionManager(atomikosUserTransaction, atomikosTransactionManager);
        }
    }


}
