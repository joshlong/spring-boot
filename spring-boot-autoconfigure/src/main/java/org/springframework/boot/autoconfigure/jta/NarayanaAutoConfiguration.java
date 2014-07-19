package org.springframework.boot.autoconfigure.jta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.TransactionManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration for JBoss TM, called <a href="http://docs.jboss.org/jbosstm/5.0.0.M6">Narayana JTA</a>.
 * Clients must register {@link org.springframework.boot.autoconfigure.jta.narayana.NarayanaXaDataSourceFactoryBean}
 * wrappers for for JDBC {@link javax.sql.DataSource datasources} and are advised to look at
 * suitable XA-aware {@link javax.jms.ConnectionFactory} implementations.
 * <a href="http://activemq.apache.org/maven/apidocs/org/apache/activemq/pool/XaPooledConnectionFactory.html">
 * The ActiveMQ project one has one that seems generic-enough, for example</a>.
 *
 * @author Josh Long
 */
@Configuration
class NarayanaAutoConfiguration extends BaseJtaAutoConfiguration {

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


    private TransactionManager jtaTransactionManager() {
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
