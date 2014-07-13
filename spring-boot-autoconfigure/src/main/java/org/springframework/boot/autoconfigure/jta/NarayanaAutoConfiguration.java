package org.springframework.boot.autoconfigure.jta;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;


@Configuration
class NarayanaAutoConfiguration {

    @Autowired(required = false)
    private JpaProperties jpaProperties;

    @Autowired(required = false)
    private JmsProperties jmsProperties;


    @Primary
    @Bean(name = "transactionManager")
    @ConditionalOnMissingBean(name = "transactionManager")
    public JtaTransactionManager transactionManager() {

        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(jtaUserTransaction(), jtaTransactionManager());
        jtaTransactionManager.setAllowCustomIsolationLevels(true);
        jtaTransactionManager.setFailEarlyOnGlobalRollbackOnly(true);
        jtaTransactionManager.setRollbackOnCommitFailure(true);

        JtaAutoConfiguration.configureJtaProperties(jtaTransactionManager, this.jmsProperties, this.jpaProperties);

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
