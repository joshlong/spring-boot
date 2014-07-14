package org.springframework.boot.autoconfigure.jta;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for JBoss TM, called <a href="http://docs.jboss.org/jbosstm/5.0.0.M6">Narayana JTA</a>.
 * Clients must register {@link org.springframework.boot.autoconfigure.jta.NarayanaDataSourceFactoryBean}
 * wrappers for  for JDBC {@link javax.sql.DataSource datasources}.
 *
 * @author Josh Long
 */
@Configuration
class NarayanaAutoConfiguration {

    private static final String NARAYANA_PROPERTIES_NAME = "narayanaProperties";

    private String narayanaProperty = "spring.jta.narayana";

    @Autowired(required = false)
    private JpaProperties jpaProperties;

    @Autowired(required = false)
    private JmsProperties jmsProperties;

    protected void configureNarayanaProperties(ConfigurableEnvironment environment) {

        // root file
        String narayana = "narayana";
        String path = JtaAutoConfiguration.jtaRootPathFor(environment, narayana);

        long timeout = Long.parseLong(environment.getProperty(narayanaProperty + ".timeout", Long.toString(60)));

        Map<String, Object> h = new HashMap<String, Object>();
        h.put("com.arjuna.ats.arjuna.coordinator.defaultTimeout", Long.toString(timeout));
        h.put("com.arjuna.ats.arjuna.objectstore.objectStoreDir", path);
        h.put("ObjectStoreEnvironmentBean.objectStoreDir", path);

        // these have to be system properties for Narayana JTA to see them.

        /* if these properties exist, then lets grab them. otherwise, use our defaults */
        for (String k : h.keySet()) {
            String v;
            if ((v = System.getProperty(k)) != null) {
                h.put(k, v);
            }
        }
        /* Now sync to System props */
        for (String k : h.keySet()) {
            System.setProperty(k, "" + h.get(k));
        }

        environment.getPropertySources().addFirst(new MapPropertySource(narayana, h));

        /*
            Now both the system properties and the environment look the same,
            with the values in the System properties taking precedence.
        */
    }

    @Bean(name = NARAYANA_PROPERTIES_NAME)
    public InitializingBean propertiesForNarayana(
            ConfigurableEnvironment environment) {

        this.configureNarayanaProperties(environment);

        return new InitializingBean() {
            @Override
            public void afterPropertiesSet() throws Exception {
                //don't care
            }
        };
    }


    @Primary
    @Bean(name = "transactionManager")
    @ConditionalOnMissingBean(name = "transactionManager")
    public JtaTransactionManager transactionManager(ConfigurableEnvironment environment) {


        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(
                this.jtaUserTransaction(), this.jtaTransactionManager());
        jtaTransactionManager.setAllowCustomIsolationLevels(true);
        jtaTransactionManager.setFailEarlyOnGlobalRollbackOnly(true);
        jtaTransactionManager.setRollbackOnCommitFailure(true);

        JtaAutoConfiguration.configureJtaProperties(jtaTransactionManager, this.jmsProperties, this.jpaProperties);

        return jtaTransactionManager;
    }

    @UserTransactionBean
    @DependsOn(NARAYANA_PROPERTIES_NAME)
    public UserTransaction jtaUserTransaction() {
        return com.arjuna.ats.jta.UserTransaction.userTransaction();
    }

    @TransactionManagerBean
    @DependsOn(NARAYANA_PROPERTIES_NAME)
    public TransactionManager jtaTransactionManager() {
        return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }


}
