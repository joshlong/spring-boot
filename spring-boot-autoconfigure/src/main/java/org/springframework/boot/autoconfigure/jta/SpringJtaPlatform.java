package org.springframework.boot.autoconfigure.jta;

import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generic Hibernate {@link org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform}
 * implementation that simply resolves the JTA {@link javax.transaction.UserTransaction} and
 * {@link javax.transaction.TransactionManager} from the Spring-configured
 * {@link org.springframework.transaction.jta.JtaTransactionManager} implementation.
 * <p>
 * A valid {@link org.springframework.transaction.jta.JtaTransactionManager} will expose
 * at a minimum a {@link javax.transaction.UserTransaction}. Sometimes a {@link javax.transaction.UserTransaction}
 * implements the {@link javax.transaction.TransactionManager} contract, and the {@link org.springframework.transaction.jta.JtaTransactionManager}
 * is smart enough to test for that.
 *
 * @author Josh Long
 */
public class SpringJtaPlatform extends AbstractJtaPlatform {

    public static final AtomicReference<JtaTransactionManager> JTA_TRANSACTION_MANAGER =
            new AtomicReference<JtaTransactionManager>();

    private static final long serialVersionUID = 1L;

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean hasTransactionManager() {
        boolean yes = JTA_TRANSACTION_MANAGER.get() != null;
        if (!yes && logger.isInfoEnabled()) {
            logger.info(
                getClass().getName()
                        + ": JTA_TRANSACTION_MANAGER.get() == null. ");
        }
        return yes;
    }

    @Override
    protected TransactionManager locateTransactionManager() {
        return (hasTransactionManager() ? JTA_TRANSACTION_MANAGER.get().getTransactionManager() : null);
    }

    @Override
    protected UserTransaction locateUserTransaction() {
        return (hasTransactionManager() ? JTA_TRANSACTION_MANAGER.get().getUserTransaction() : null);
    }


}
