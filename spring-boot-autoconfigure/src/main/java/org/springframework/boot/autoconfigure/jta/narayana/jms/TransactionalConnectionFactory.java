package org.springframework.boot.autoconfigure.jta.narayana.jms;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.jms.connection.SessionProxy;
import org.springframework.util.Assert;

import javax.jms.*;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;


/**
 * This is a wrapper-{@link javax.jms.ConnectionFactory} that can participate in Naryana JTA transactions.
 * (The things I do for love and Spring!)
 * <p>
 * This borrows heavily in design and spirit from Spring's JMS support, in particular, from
 * {@link org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy}
 * which makes plain JMS connections aware of Spring-managed transactions (using {@link org.springframework.transaction.support.TransactionSynchronization} and the like.)
 * <p>
 * This differs from the the approach taken in {@link org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy}, instead of synchronizing JMS operations with transaction state held by Spring,
 * this class will synchronize JMS operations with transaction state held by JTA. Which Spring can then manage. This is
 * also a fair bit more complex in that it manages XA transaction enlistment with {@link javax.transaction.xa.XAResource}.
 *
 * @author Josh Long
 */
public class TransactionalConnectionFactory implements
        ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory {

    private final ConnectionFactory targetConnectionFactory;

    public TransactionalConnectionFactory(ConnectionFactory cf) {
        this.targetConnectionFactory = cf;
        Assert.isTrue(cf instanceof XAConnectionFactory,
                "the targetConnectionFactory must also support " + javax.jms.XAConnectionFactory.class.getName()
                        + ", or an xaConnectionFactory must be provided");
    }

    /**
     * builds a {@link javax.jms.Connection} that is aware of, and participates in, the surrounding JTA transaction, if available.
     */
    protected <CF extends ConnectionFactory, C extends Connection> C getTransactionAwareConnectionProxy(CF connectionFactory, C target) {

        MethodInterceptor advice = new TransactionAwareConnectionMethodInterceptor(this.targetConnectionFactory, target);
        ProxyFactoryBean pfb = new ProxyFactoryBean();
        pfb.setAutodetectInterfaces(true);
        pfb.setProxyTargetClass(true);
        pfb.addAdvice(advice);
        pfb.setTarget(target);
        pfb.addInterface(Connection.class);
        if (target instanceof QueueConnection) {
            pfb.addInterface(QueueConnection.class);
        }
        if (target instanceof TopicConnection) {
            pfb.addInterface(TopicConnection.class);
        }
        return (C) pfb.getObject();
    }

    @Override
    public Connection createConnection() throws JMSException {
        return this.getTransactionAwareConnectionProxy(
                this.targetConnectionFactory,
                this.targetConnectionFactory.createConnection());
    }

    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        return this.getTransactionAwareConnectionProxy(
                this.targetConnectionFactory,
                this.targetConnectionFactory.createConnection(userName, password));
    }

    @Override
    public QueueConnection createQueueConnection() throws JMSException {
        return this.getTransactionAwareConnectionProxy(this.targetConnectionFactory,
                ((QueueConnectionFactory) this.targetConnectionFactory).createQueueConnection());
    }

    @Override
    public QueueConnection createQueueConnection(String userName, String password) throws JMSException {
        return this.getTransactionAwareConnectionProxy(this.targetConnectionFactory,
                ((QueueConnectionFactory) this.targetConnectionFactory).createQueueConnection(userName, password));
    }

    @Override
    public TopicConnection createTopicConnection() throws JMSException {
        return this.getTransactionAwareConnectionProxy(this.targetConnectionFactory,
                ((TopicConnectionFactory) this.targetConnectionFactory).createTopicConnection());
    }

    @Override
    public TopicConnection createTopicConnection(String userName, String password) throws JMSException {
        return this.getTransactionAwareConnectionProxy(this.targetConnectionFactory,
                ((TopicConnectionFactory) this.targetConnectionFactory).createTopicConnection(userName, password));
    }


    /**
     * Invocation handler that exposes transactional Sessions for the underlying Connection.
     */
    private static class TransactionAwareConnectionMethodInterceptor
            implements MethodInterceptor {

        private final Connection connection;

        private final ConnectionFactory connectionFactory;

        public TransactionAwareConnectionMethodInterceptor(
                ConnectionFactory connectionFactory,
                Connection targetConnection) {
            this.connectionFactory = connectionFactory;
            this.connection = targetConnection;
        }

        /*public static Session doGetTransactionalSession(
            ConnectionFactory connectionFactory, ResourceFactory resourceFactory, boolean startConnection)
			throws JMSException {

		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		Assert.notNull(resourceFactory, "ResourceFactory must not be null");

		JmsResourceHolder resourceHolder =
				(JmsResourceHolder) TransactionSynchronizationManager.getResource(connectionFactory);
		if (resourceHolder != null) {
			Session session = resourceFactory.getSession(resourceHolder);
			if (session != null) {
				if (startConnection) {
					Connection con = resourceFactory.getConnection(resourceHolder);
					if (con != null) {
						con.start();
					}
				}
				return session;
			}
			if (resourceHolder.isFrozen()) {
				return null;
			}
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return null;
		}
		JmsResourceHolder resourceHolderToUse = resourceHolder;
		if (resourceHolderToUse == null) {
			resourceHolderToUse = new JmsResourceHolder(connectionFactory);
		}
		Connection con = resourceFactory.getConnection(resourceHolderToUse);
		Session session = null;
		try {
			boolean isExistingCon = (con != null);
			if (!isExistingCon) {
				con = resourceFactory.createConnection();
				resourceHolderToUse.addConnection(con);
			}
			session = resourceFactory.createSession(con);
			resourceHolderToUse.addSession(session, con);
			if (startConnection) {
				con.start();
			}
		}
		catch (JMSException ex) {
			if (session != null) {
				try {
					session.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			if (con != null) {
				try {
					con.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			throw ex;
		}
		if (resourceHolderToUse != resourceHolder) {
			TransactionSynchronizationManager.registerSynchronization(
					new JmsResourceSynchronization(
							resourceHolderToUse, connectionFactory, resourceFactory.isSynchedLocalTransactionAllowed()));
			resourceHolderToUse.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.bindResource(connectionFactory, resourceHolderToUse);
		}
		return session;
	}
*/

        // todo
        private TopicSession getTransactionalTopicSession(TopicConnectionFactory connectionFactory, TopicConnection connection) {
            return null;
        }

        // todo
        private QueueSession getTransactionalQueueSession(QueueConnectionFactory connectionFactory, QueueConnection connection) {
            return null;
        }

        // todo
        private Session getTransactionalSession(ConnectionFactory connectionFactory, Connection connection) {
            return null;
        }

        private Session getCloseSuppressingSessionProxy(Session target) {
            List<Class<?>> classes = new ArrayList<Class<?>>(3);
            classes.add(SessionProxy.class);
            if (target instanceof QueueSession) {
                classes.add(QueueSession.class);
            }
            if (target instanceof TopicSession) {
                classes.add(TopicSession.class);
            }
            return (Session) Proxy.newProxyInstance(
                    SessionProxy.class.getClassLoader(),
                    classes.toArray(new Class<?>[classes.size()]),
                    new CloseSuppressingSessionInvocationHandler(target));
        }

        @Override
        public Object invoke(MethodInvocation methodInvocation) throws Throwable {
            Method method = methodInvocation.getMethod();
            Object proxy = methodInvocation.getThis();
            Object[] args = methodInvocation.getArguments();
            if (method.getName().equals("equals")) {
                return (proxy == args[0]);
            } else if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            } else if (Session.class.equals(method.getReturnType())) {
                return getTransactionalSession(connectionFactory, this.connection);
            } else if (QueueSession.class.equals(method.getReturnType())) {
                return getTransactionalQueueSession((QueueConnectionFactory) this.connectionFactory, (QueueConnection) this.connection);
            } else if (TopicSession.class.equals(method.getReturnType())) {
                return getTransactionalTopicSession((TopicConnectionFactory) this.connectionFactory, (TopicConnection) this.connection);
            }
            try {
                return method.invoke(this.connection, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }


        /**
         * Invocation handler that suppresses close calls for a transactional JMS Session.
         */
        private static class CloseSuppressingSessionInvocationHandler implements InvocationHandler {

            private final Session target;

            public CloseSuppressingSessionInvocationHandler(Session target) {
                this.target = target;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // Invocation on SessionProxy interface coming in...

                if (method.getName().equals("equals")) {
                    // Only consider equal when proxies are identical.
                    return (proxy == args[0]);
                } else if (method.getName().equals("hashCode")) {
                    // Use hashCode of Connection proxy.
                    return System.identityHashCode(proxy);
                } else if (method.getName().equals("commit")) {
                    throw new TransactionInProgressException("Commit call not allowed within a managed transaction");
                } else if (method.getName().equals("rollback")) {
                    throw new TransactionInProgressException("Rollback call not allowed within a managed transaction");
                } else if (method.getName().equals("close")) {
                    // Handle close method: not to be closed within a transaction.
                    return null;
                } else if (method.getName().equals("getTargetSession")) {
                    // Handle getTargetSession method: return underlying Session.
                    return this.target;
                }

                // Invoke method on target Session.
                try {
                    return method.invoke(this.target, args);
                } catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
            }
        }
    }
/*
    public static Session doGetTransactionalSession(
            ConnectionFactory connectionFactory, ResourceFactory resourceFactory, boolean startConnection)
            throws JMSException {

        Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
        Assert.notNull(resourceFactory, "ResourceFactory must not be null");

        JmsResourceHolder resourceHolder =
                (JmsResourceHolder) TransactionSynchronizationManager.getResource(connectionFactory);
        if (resourceHolder != null) {
            Session session = resourceFactory.getSession(resourceHolder);
            if (session != null) {
                if (startConnection) {
                    Connection con = resourceFactory.getConnection(resourceHolder);
                    if (con != null) {
                        con.start();
                    }
                }
                return session;
            }
            if (resourceHolder.isFrozen()) {
                return null;
            }
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return null;
        }
        JmsResourceHolder resourceHolderToUse = resourceHolder;
        if (resourceHolderToUse == null) {
            resourceHolderToUse = new JmsResourceHolder(connectionFactory);
        }
        Connection con = resourceFactory.getConnection(resourceHolderToUse);
        Session session = null;
        try {
            boolean isExistingCon = (con != null);
            if (!isExistingCon) {
                con = resourceFactory.createConnection();
                resourceHolderToUse.addConnection(con);
            }
            session = resourceFactory.createSession(con);
            resourceHolderToUse.addSession(session, con);
            if (startConnection) {
                con.start();
            }
        }
        catch (JMSException ex) {
            if (session != null) {
                try {
                    session.close();
                }
                catch (Throwable ex2) {
                    // ignore
                }
            }
            if (con != null) {
                try {
                    con.close();
                }
                catch (Throwable ex2) {
                    // ignore
                }
            }
            throw ex;
        }
        if (resourceHolderToUse != resourceHolder) {
            TransactionSynchronizationManager.registerSynchronization(
                    new JmsResourceSynchronization(
                            resourceHolderToUse, connectionFactory, resourceFactory.isSynchedLocalTransactionAllowed()));
            resourceHolderToUse.setSynchronizedWithTransaction(true);
            TransactionSynchronizationManager.bindResource(connectionFactory, resourceHolderToUse);
        }
        return session;
    }*/

}


abstract class Registry {

    public static void rollback() {
        try {
            transaction().setRollbackOnly();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    public static XAResource getXaResourceFromConnection(Connection connection) {
        XAConnection xaConnection = (XAConnection) connection;

        return null;
    }

    public static javax.transaction.Transaction transaction() {
        try {
            return com.arjuna.ats.jta.TransactionManager
                    .transactionManager().getTransaction();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    public static void validate() throws Exception {
        if (transaction() == null) {
            return;
        }
        Assert.isTrue(transaction().getStatus() == Status.STATUS_ACTIVE,
                "you must participate in a valid transaction");
    }

    public static XAResource register(Connection jmsConnection)
            throws RuntimeException, RollbackException, SystemException {
        Assert.notNull(jmsConnection, "the JMS connection must not be null");
        Transaction transaction = transaction();
        XAResource resource = getXaResourceFromConnection(jmsConnection);
        if (!((com.arjuna.ats.jta.transaction.Transaction) transaction).enlistResource(resource, new Object[0])) {
            rollback();
        }
        return resource;
    }

}




