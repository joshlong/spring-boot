package org.springframework.boot.autoconfigure.jta.bitronix;

import bitronix.tm.resource.jms.PoolingConnectionFactory;

import javax.jms.XAConnectionFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers a Bitronix {@link bitronix.tm.resource.jms.PoolingConnectionFactory} that will participate
 * in Bitronix-managed XA transactions.
 *
 * @param <XA> the type of the {@link javax.jms.XAConnectionFactory} subclass that you want to use.
 * @author Josh Long
 */
public abstract class BitronixConnectionFactoryFactoryBean
        <XA extends XAConnectionFactory> extends BaseBitronixResourceFactoryBean<XA, PoolingConnectionFactory> {

    public BitronixConnectionFactoryFactoryBean(Class<XA> xaConnectionFactoryClass) {
        super(xaConnectionFactoryClass, PoolingConnectionFactory.class);
    }

    @Override
    public PoolingConnectionFactory build() throws Exception {

        PoolingConnectionFactory poolingDataSource = new PoolingConnectionFactory();

        Map<String, Object> recordedProperties = new ConcurrentHashMap<String, Object>();
        XA recordingDataSource = PropertyRecordingProxyUtils.getPropertyRecordingConnectionFactory(
                this.getXaDriverClass(), recordedProperties);

        this.configureXaResource(recordingDataSource);

        poolingDataSource.setClassName(this.getXaDriverClass ().getName());
        poolingDataSource.setMaxPoolSize(10);
        poolingDataSource.setUniqueName(getUniqueNodeName());
        poolingDataSource.getDriverProperties().putAll(recordedProperties);
        poolingDataSource.setTestConnections(true);
        poolingDataSource.setAutomaticEnlistingEnabled(true);
        poolingDataSource.setAllowLocalTransactions(true);
        poolingDataSource.init();

        return poolingDataSource;

    }

}
