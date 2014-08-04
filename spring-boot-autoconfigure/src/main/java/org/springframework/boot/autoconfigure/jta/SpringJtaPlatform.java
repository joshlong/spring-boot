package org.springframework.boot.autoconfigure.jta;

import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Generic Hibernate {@link AbstractJtaPlatform} implementation that simply resolves the
 * JTA {@link UserTransaction} and {@link TransactionManager} from the Spring-configured
 * {@link JtaTransactionManager} implementation.
 * <p>
 * A valid {@link JtaTransactionManager} will expose at a minimum a
 * {@link javax.transaction.UserTransaction}. Sometimes a
 * {@link javax.transaction.UserTransaction} implements the
 * {@link javax.transaction.TransactionManager} contract, and the
 * {@link JtaTransactionManager} is smart enough to test for that.
 *
 * @author Josh Long
 */
public class SpringJtaPlatform extends AbstractJtaPlatform {

	public static final AtomicReference<JtaTransactionManager> JTA_TRANSACTION_MANAGER = new AtomicReference<JtaTransactionManager>();

	private static final long serialVersionUID = 1L;

	private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

	protected boolean hasTransactionManager() {
		boolean yes = JTA_TRANSACTION_MANAGER.get() != null;
		if (!yes && this.logger.isInfoEnabled()) {
			this.logger.info(getClass().getName()
					+ ": JTA_TRANSACTION_MANAGER.get() == null. ");
		}
		return yes;
	}

	@Override
	protected TransactionManager locateTransactionManager() {
		return (hasTransactionManager() ? JTA_TRANSACTION_MANAGER.get()
				.getTransactionManager() : null);
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (hasTransactionManager() ? JTA_TRANSACTION_MANAGER.get()
				.getUserTransaction() : null);
	}

}
