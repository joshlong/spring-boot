package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
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
@ConditionalOnMissingBean(name = "transactionManager", value = PlatformTransactionManager.class)
@AutoConfigureAfter({ ArjunaAutoConfiguration.class, BitronixAutoConfiguration.class,
		AtomikosAutoConfiguration.class })
@AutoConfigureBefore(JmsAutoConfiguration.class)
public class JavaEeAutoConfiguration extends AbstractJtaAutoConfiguration {

	@Override
	protected JtaTransactionManager buildJtaTransactionManager() throws Exception {
		JtaTransactionManager txManager = new JtaTransactionManager();
		txManager.setAutodetectTransactionManager(true);
		txManager.setAllowCustomIsolationLevels(true);

		return txManager;
	}
}
