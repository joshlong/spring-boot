package org.springframework.boot.autoconfigure.jta;


import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Creates the {@link org.springframework.transaction.jta.JtaTransactionManager}
 * by deferring to JNDI-bound {@link javax.transaction.UserTransaction}
 * and {@link javax.transaction.TransactionManager}
 *
 * @author Josh Long
 */
@Configuration
class JndiAutoConfiguration extends BaseJtaAutoConfiguration {

    @Override
    protected JtaTransactionManager buildJtaTransactionManager() throws Exception {
        return new JtaTransactionManager();
    }
}
