package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

/**
 * Are we running in an environment that has basic JTA-capable types on the CLASSPATH
 * <EM>and</EM> that has JTA-infrastructure beans, like a
 * {@link javax.transaction.UserTransaction} and a
 * {@link javax.transaction.TransactionManager}. This only proves the basics of JTA are
 * there. Use in conjunction with other tests.
 *
 * @author Josh Long
 * @since 1.2.0
 */
public class JtaCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ClassLoader classLoader = context.getClassLoader();

		// make sure we have both JTA transaction manager types of interest
		boolean jtaTransactionManagerTypesOnClassPath = ClassUtils.isPresent(
				"javax.transaction.UserTransaction", classLoader)
				&& ClassUtils.isPresent("javax.transaction.TransactionManager",
						classLoader);

		// and a type that can be used as a resource (such as an XADataSource or a
		// XAConnectionFactory)
		boolean jtaResourceTypeOnClassPath = ClassUtils.isPresent(
				"javax.jms.XAConnectionFactory", classLoader)
				|| ClassUtils.isPresent("javax.sql.XADataSource", classLoader);

		if (jtaTransactionManagerTypesOnClassPath && jtaResourceTypeOnClassPath) {
			return ConditionOutcome.match();
		}

		return ConditionOutcome.noMatch("no XA class.");
	}
}
