package org.springframework.boot.autoconfigure.jta;

import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.AtomikosConnectionFactoryBean;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.junit.Test;
import org.postgresql.xa.PGXADataSource;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.jta.JtaTransactionManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Josh Long
 */
public class JtaAutoConfigurationTests {

    private AnnotationConfigApplicationContext context;

    @Configuration
    @Import({JtaAutoConfiguration.DefaultJtaConfiguration.class,
            JmsAutoConfiguration.class, AtomikosAutoConfiguration.class})
    public static class ExampleAtomikisConfiguration {

        @Bean(initMethod = "init", destroyMethod = "close")
        public AtomikosDataSourceBean xaDataSource() {

            AtomikosDataSourceBean xaDS = new AtomikosDataSourceBean();
            xaDS.setUniqueResourceName("xaDataSource");
            xaDS.setTestQuery("select now()");
            xaDS.setXaDataSource(dataSource("127.0.0.1", "crm", "crm", "crm"));
            xaDS.setPoolSize(10);

            return xaDS;
        }

        @Bean(initMethod = "init", destroyMethod = "close")
        public AtomikosConnectionFactoryBean xaConnectionFactory() {
            AtomikosConnectionFactoryBean xaCF = new AtomikosConnectionFactoryBean();
            xaCF.setXaConnectionFactory(connectionFactory("tcp://localhost:61616"));
            xaCF.setUniqueResourceName("xaConnectionFactory");
            xaCF.setPoolSize(10);
            xaCF.setLocalTransactionMode(false);
            return xaCF;
        }

        private javax.jms.XAConnectionFactory connectionFactory(String url) {
            return new ActiveMQXAConnectionFactory(url);
        }

        private javax.sql.XADataSource dataSource(String host, String db, String username, String pw) {
            PGXADataSource pgxaDataSource = new PGXADataSource();
            pgxaDataSource.setServerName(host);
            pgxaDataSource.setDatabaseName(db);
            pgxaDataSource.setUser(username);
            pgxaDataSource.setPassword(pw);
            return pgxaDataSource;
        }
    }

    @Test
    public void testDefaultAtomikosConfiguration() {


        this.context = new AnnotationConfigApplicationContext(ExampleAtomikisConfiguration.class);
        this.context.start();

        JtaTransactionManager jtaTransactionManager = this.context.getBean(JtaTransactionManager.class);
        JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);

        assertNotNull("the jtaTransactionManager should not be null!", jtaTransactionManager);
        assertTrue("transactionManager should be an Atomikos implementation.", jtaTransactionManager.getTransactionManager() instanceof UserTransactionManager);
        assertNotNull("the jmsTemplate should not be null!", jmsTemplate);

    }

}
