package org.springframework.boot.autoconfigure.jta;

import com.atomikos.icatch.admin.LogAdministrator;
import com.atomikos.icatch.standalone.UserTransactionServiceFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
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

        // @Autowired
        public void customizeHibernateIfRequired(ConfigurableApplicationContext configurableApplicationContext) {
            //properties.set("hibernate.transaction.manager_lookup_class", TransactionManagerLookup.class.getName());
        }


        /*	<bean id="broker" class="org.apache.activemq.xbean.BrokerFactoryBean" >
		<property name="config" value="classpath:activemq.xml" />
		<property name="start" value="true" />
	</bean>
	<!-- ATE -->
	<bean id="atomikosTransactionManager" class="com.atomikos.icatch.jta.UserTransactionManager"
		init-method="init" destroy-method="close" depends-on="dataSource,connectionFactory">
		<property name="forceShutdown" value="false" />
	</bean>
	<bean id="atomikosUserTransaction" class="com.atomikos.icatch.jta.UserTransactionImp">
		<property name="transactionTimeout" value="300" />
	</bean>
	<bean id="placeholderConfig"
		class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer">
	</bean>


	<bean id="publisher" class="com.atomikos.demo.publisher.Publisher">
		<property name="jmsTemplate" ref="jmsTemplate" />
		<property name="xmlFoldername" value="xml" />
	</bean>


	<bean id="jmsTemplate" class="org.springframework.jms.core.JmsTemplate">
		<property name="connectionFactory" ref="connectionFactory" />
		<property name="defaultDestination" ref="queue" />
		<property name="receiveTimeout" value="100" />
		<property name="sessionTransacted" value="true" />
	</bean>

	<bean id="messageDrivenContainer" class="com.atomikos.jms.extra.MessageDrivenContainer" depends-on="atomikosTransactionManager" init-method="start" destroy-method="stop">
		<property name="atomikosConnectionFactoryBean" ref="connectionFactory" />
		<property name="transactionTimeout" value="10" />
		<property name="poolSize" value="10" />
		<property name="destination" ref="queue" />
		<property name="messageListener" ref="messageListener" />
	</bean>

	<bean id="messageListener"
		class="com.atomikos.demo.transformer.HibernatePersistingMessageListenerImpl">
		<property name="sessionFactory" ref="sessionFactory" />
	</bean>


	<bean id="queue" class="org.apache.activemq.command.ActiveMQQueue">
		<property name="physicalName" value="ato-test-queue" />
	</bean>

	<bean id="connectionFactory" class="com.atomikos.jms.AtomikosConnectionFactoryBean"
		init-method="init" destroy-method="close" depends-on="broker">
		<property name="uniqueResourceName" value="amq1" />
		<property name="maxPoolSize" value="20" />
		<property name="xaConnectionFactory">
			<bean class="org.apache.activemq.ActiveMQXAConnectionFactory">
				<property name="brokerURL" value="tcp://localhost:61616" />
			</bean>
		</property>
	</bean>

	<bean id="dataSource" class="com.atomikos.jdbc.AtomikosDataSourceBean"
		init-method="init" destroy-method="close">
		<property name="uniqueResourceName" value="db" />
		<property name="xaDataSourceClassName" value="org.postgresql.xa.PGXADataSource" />
		<property name="xaProperties">
			<props>
				<!--<prop key="databaseName">${basedir}/target/classes/db</prop>-->
				<prop key="ServerName">localhost</prop>
				<prop key="PortNumber">5432</prop>
				<prop key="DatabaseName">atomikos</prop>
				<prop key="User">atomikos</prop>
				<prop key="Password">atomikos</prop>

			</props>
		</property>
		<!--<property name="minPoolSize" value="1" />-->
		<property name="maxPoolSize" value="20" />
		<property name="minPoolSize" value="10"/>
		<property name="testQuery" value="select now()"/>
	</bean>


	<!--<bean id="dataSource" class="com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean"
		init-method="init" destroy-method="close">
		<property name="driverClassName" value="org.apache.derby.jdbc.EmbeddedDriver" />
		<property name="uniqueResourceName" value="db" />
		<property name="url" value="jdbc:derby:memory:db;create=true" />
		<property name="user" value="derbyuser" />
		<property name="password" value="derbyuser" />
		<property name="maxPoolSize" value="1" />
		<property name="testQuery" value="select schemaname from sys.sysschemas"/>
	</bean>-->

	<!-- Spring TM config -->
	<tx:annotation-driven transaction-manager="transactionManager" />

	<bean id="transactionManager"
		class="org.springframework.transaction.jta.JtaTransactionManager">
		<property name="transactionManager" ref="atomikosTransactionManager" />
		<property name="userTransaction" ref="atomikosUserTransaction" />
	</bean>

	<!-- Hibernate -->
	<bean id="sessionFactory"
		class="org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean">
		<property name="annotatedClasses">
			<list>
				<value>com.atomikos.demo.domain.Order</value>
			</list>
		</property>
		<property name="dataSource" ref="dataSource" />
		<property name="hibernateProperties">
			<props>
				<prop key="hibernate.dialect">org.hibernate.dialect.PostgreSQLDialect</prop>
				<prop key="hibernate.current_session_context_class">jta</prop>
				<prop key="hibernate.transaction.factory_class">com.atomikos.icatch.jta.hibernate3.AtomikosJTATransactionFactory
				</prop>
				<prop key="hibernate.transaction.manager_lookup_class">com.atomikos.icatch.jta.hibernate3.TransactionManagerLookup
				</prop>
				<prop key="hibernate.hbm2ddl.auto">create</prop>
			</props>
		</property>
	</bean>

*/


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
            properties.setProperty("com.atomikos.icatch", UserTransactionServiceFactory.class.getName());
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
