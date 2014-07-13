package org.springframework.boot.autoconfigure.jta;

import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.annotation.PostConstruct;
 import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.util.concurrent.atomic.AtomicReference;

public class SpringJtaPlatform extends AbstractJtaPlatform {


    public static final AtomicReference<JtaTransactionManager> JTA_TRANSACTION_MANAGER =
            new AtomicReference<JtaTransactionManager>();

    private static final long serialVersionUID = 1L;

    private boolean hasTransactionManager() {
        return JTA_TRANSACTION_MANAGER.get() != null;
    }

    @Override
    protected TransactionManager locateTransactionManager() {
        return (hasTransactionManager() ? JTA_TRANSACTION_MANAGER.get().getTransactionManager() : null);
    }

    @Override
    protected UserTransaction locateUserTransaction() {
        return (hasTransactionManager() ? JTA_TRANSACTION_MANAGER.get().getUserTransaction() : null);
    }

    @Autowired
    private ObjectFactory<JtaTransactionManager> transactionManager;

    @PostConstruct
    public void setup() {
        JTA_TRANSACTION_MANAGER.set(
                this.transactionManager.getObject() );
    }

}
