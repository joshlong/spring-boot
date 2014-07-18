package org.springframework.boot.autoconfigure.jta.narayana.jms;

import org.springframework.beans.factory.FactoryBean;

import javax.jms.ConnectionFactory;

/**
 * Returns an object that can act as a {@link javax.jms.ConnectionFactory} but that
 * correctly enlists and delists with the JBoss TM JTA implementation.
 *
 * @author Josh Long
 */
public class TransactionalConnectionFactoryFactoryBean
        implements FactoryBean<ConnectionFactory> {

    private ConnectionFactory connectionFactory;

    public TransactionalConnectionFactoryFactoryBean(ConnectionFactory xaConnectionFactory) {
        this.connectionFactory = xaConnectionFactory;
    }

    @Override
    public ConnectionFactory getObject() throws Exception {
        return new TransactionalConnectionFactory(
                this.connectionFactory);
    }

    @Override
    public Class<?> getObjectType() {
        return ConnectionFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
