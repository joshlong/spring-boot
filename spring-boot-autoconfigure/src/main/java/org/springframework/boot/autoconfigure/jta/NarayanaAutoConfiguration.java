package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;


@Configuration
public class NarayanaAutoConfiguration {

    @Primary
    @Bean(name = "transactionManager")
    @ConditionalOnMissingBean(name = "transactionManager")
    public JtaTransactionManager transactionManager(
            JtaTransactionManagerConfigurer[] jtaTransactionManagerConfigurers) {

        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(jtaUserTransaction(), jtaTransactionManager());
        jtaTransactionManager.setAllowCustomIsolationLevels(true);
        jtaTransactionManager.setFailEarlyOnGlobalRollbackOnly(true);
        jtaTransactionManager.setRollbackOnCommitFailure(true);

        for (JtaTransactionManagerConfigurer c : jtaTransactionManagerConfigurers)
            c.configureJtaTransactionManager(jtaTransactionManager);

        return jtaTransactionManager;
    }

    @UserTransactionBean
    public UserTransaction jtaUserTransaction() {
        return com.arjuna.ats.jta.UserTransaction.userTransaction();
    }

    @TransactionManagerBean
    public TransactionManager jtaTransactionManager() {
        return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }


}
