package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

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
 */
@Configuration
@Conditional(JtaCondition.class)
@ConditionalOnClass(name = "javax.ejb.Singleton")
@ConditionalOnMissingBean(name = "transactionManager", value = PlatformTransactionManager.class)
@AutoConfigureAfter({ BitronixJtaAutoConfiguration.class,
		AtomikosJtaAutoConfiguration.class })
public class JndiJtaAutoConfiguration extends AbstractJtaAutoConfiguration {

	@Override
	protected JtaTransactionManager buildJtaTransactionManager() throws Exception {
		JtaTransactionManager txManager = new JtaTransactionManager();
		txManager.setAutodetectTransactionManager(true);
		txManager.setAllowCustomIsolationLevels(true);
		return txManager;
	}
}
