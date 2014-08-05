package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the standard
 * {@link org.springframework.transaction.jta.JtaTransactionManager} and configures JTA
 * support by looking up the required {@link javax.transaction.UserTransaction} and
 * {@link javax.transaction.TransactionManager} in JNDI in well-known (and, often,
 * not-so-well-known) locations. This assumes a full Java EE application server
 * environment. In this case, resource-enlistment should happen naturally assuming you're
 * talking to an appropriate {@link javax.sql.DataSource} or
 * {@link javax.jms.ConnectionFactory}.
 *
 * @author Josh Long
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass(name = "javax.ejb.Singleton")
public class JndiJtaAutoConfiguration {

	// @Override
	// protected JtaTransactionManager buildJtaTransactionManager() throws Exception {
	// JtaTransactionManager txManager = new JtaTransactionManager();
	// txManager.setAutodetectTransactionManager(true);
	// txManager.setAllowCustomIsolationLevels(true);
	// return txManager;
	// }

}
