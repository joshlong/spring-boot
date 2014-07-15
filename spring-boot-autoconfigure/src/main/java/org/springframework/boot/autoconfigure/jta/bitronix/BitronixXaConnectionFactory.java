package org.springframework.boot.autoconfigure.jta.bitronix;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;

import javax.jms.XAConnectionFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BitronixXaConnectionFactory<T extends XAConnectionFactory>
        implements BeanNameAware, FactoryBean<PoolingConnectionFactory> {
    private final Class<T> xaDataSourceClass;
    private String uniqueNodeName;

    public BitronixXaConnectionFactory(Class<T> xaDataSourceClass) {
        this.xaDataSourceClass = xaDataSourceClass;
    }

    protected PoolingConnectionFactory buildXaConnectionFactory(
            String uniqueNodeName, Class<T> xaDataSourceClassName)
            throws IllegalAccessException, InstantiationException {

        PoolingConnectionFactory poolingDataSource = new PoolingConnectionFactory();

        Map<String, Object> recordedProperties = new ConcurrentHashMap<String, Object>();
        T recordingDataSource = PropertyRecordingProxyUtils.buildPropertyRecordingConnectionFactory(xaDataSourceClassName, recordedProperties);
        configureXaConnectionFactory(recordingDataSource);

        poolingDataSource.setClassName(xaDataSourceClassName.getName());
        poolingDataSource.setMaxPoolSize(10);
        poolingDataSource.setUniqueName(uniqueNodeName);
        poolingDataSource.getDriverProperties().putAll(recordedProperties);
        poolingDataSource.init();

        return poolingDataSource;
    }

    @Override
    public void setBeanName(String name) {
        this.uniqueNodeName = name + Long.toString(System.currentTimeMillis());
    }

    @Override
    public PoolingConnectionFactory getObject() throws Exception {
        return this.buildXaConnectionFactory(this.uniqueNodeName, this.xaDataSourceClass);
    }

    @Override
    public Class<?> getObjectType() {
        return PoolingConnectionFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * client will be given a proxy to the instance which will record property configurations
     * and pass them through to the underlying {@link javax.jms.XAConnectionFactory}
     */
    protected abstract void configureXaConnectionFactory(T xaDataSource);
}
