package org.springframework.boot.autoconfigure.jta.bitronix;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import javax.jms.XAConnectionFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

 @Deprecated
 abstract class BitronixXaConnectionFactoryFactoryBean<MQ extends XAConnectionFactory>
        implements InitializingBean, BeanNameAware, FactoryBean<PoolingConnectionFactory> {

    /**
     * We need this so that we can act at runtime on types of our {@link javax.sql.DataSource}.
     * (#erasureproblems!)
     */
    private final Class<MQ> xaDataSourceClass;

    /**
     * Bitronix requires the concept of a node. We provide one using the
     * bean's name plus the current time.
     */
    private String uniqueNodeName;

    public BitronixXaConnectionFactoryFactoryBean(Class<MQ> xaDataSourceClass) {
        this.xaDataSourceClass = xaDataSourceClass;
    }

    @Override
    public void setBeanName(String name) {
        this.uniqueNodeName = name + Long.toString(System.currentTimeMillis());
        LoggerFactory.getLogger(getClass()).info("uniqueNodeName: " + this.uniqueNodeName);
    }

    @Override
    public PoolingConnectionFactory getObject() throws Exception {
        return this.poolingConnectionFactory ;
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
     * clients are given a callback during which they
     * may configure the {@link javax.sql.DataSource}
     * instance of their choosing.
     */
    protected abstract void configureXaConnectionFactory(MQ xaDataSource);

    protected PoolingConnectionFactory buildXaConnectionFactory(
            String uniqueNodeName, Class<MQ> xaDataSourceClassName)
            throws IllegalAccessException, InstantiationException {

        PoolingConnectionFactory poolingDataSource = new PoolingConnectionFactory();

        Map<String, Object> recordedProperties = new ConcurrentHashMap<String, Object>();
        MQ recordingDataSource = PropertyRecordingProxyUtils.getPropertyRecordingConnectionFactory(xaDataSourceClassName, recordedProperties);
        configureXaConnectionFactory(recordingDataSource);

        poolingDataSource.setClassName(xaDataSourceClassName.getName());
        poolingDataSource.setMaxPoolSize(10);
        poolingDataSource.setUniqueName(uniqueNodeName);
        poolingDataSource.getDriverProperties().putAll(recordedProperties);
        poolingDataSource.setTestConnections(true);
        poolingDataSource.setAutomaticEnlistingEnabled(true);
        poolingDataSource.setAllowLocalTransactions(true);
        poolingDataSource.init();

        return poolingDataSource;
    }

    private   PoolingConnectionFactory poolingConnectionFactory ;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.poolingConnectionFactory = this.buildXaConnectionFactory(this.uniqueNodeName, this.xaDataSourceClass);
    }
}
