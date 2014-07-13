package org.springframework.boot.autoconfigure.jta;

import org.springframework.transaction.jta.JtaTransactionManager;

/**
* Created by jlong on 7/13/14.
*/
public interface JtaTransactionManagerConfigurer {
    void configureJtaTransactionManager(JtaTransactionManager jtaTransactionManager);
}
