package org.springframework.boot.autoconfigure.jta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.TransactionManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers the <A href="http://docs.jboss.org/jbosstm/">Narayana JTA</a> implementation and
 * configures JTA support. Clients may register their {@link javax.sql.DataSource}s
 * with the {@link com.arjuna.ats.jdbc.TransactionalDriver}-wrapping
 * {@link org.springframework.boot.autoconfigure.jta.narayana.NarayanaXaDataSourceFactoryBean}
 * and their JMS {@link javax.jms.ConnectionFactory}s with a JTA-aware {@link javax.jms.ConnectionFactory}
 * proxy, like <a href="http://activemq.apache.org/maven/apidocs/org/apache/activemq/pool/XaPooledConnectionFactory.html">
 * the ActiveMQ project's <code>XaPooledConnectionFactory</code></a>
 *
 * @author Josh Long
 */
@Configuration
class NarayanaAutoConfiguration extends AbstractJtaAutoConfiguration {

    private String narayanaProperty = "spring.jta.narayana";

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


    @Bean
    @ConditionalOnMissingBean
    public TransactionManager jtaTransactionManager() {
        configureNarayanaProperties(this.environment);
        return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }

    @Autowired
    private ConfigurableEnvironment environment;

    @Override
    protected JtaTransactionManager buildJtaTransactionManager() throws Exception {
        return new JtaTransactionManager(this.jtaTransactionManager());
    }
}
