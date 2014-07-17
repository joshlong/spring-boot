package org.springframework.boot.autoconfigure.jta.bitronix;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;

import javax.jms.XAConnectionFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BitronixXaConnectionFactory<DS extends XAConnectionFactory>
        implements BeanNameAware, FactoryBean<PoolingConnectionFactory> {

    /**
     * We need this so that we can act at runtime on types of our {@link javax.sql.DataSource}.
     * (#erasureproblems!)
     */
    private final Class<DS> xaDataSourceClass;

    /**
     * Bitronix requires the concept of a node. We provide one using the
     * bean's name plus the current time.
     */
    private String uniqueNodeName;

    public BitronixXaConnectionFactory(Class<DS> xaDataSourceClass) {
        this.xaDataSourceClass = xaDataSourceClass;
    }

    @Override
    public void setBeanName(String name) {
        this.uniqueNodeName = name + Long.toString(System.currentTimeMillis());
        LoggerFactory.getLogger(getClass()).info("uniqueNodeName: " + this.uniqueNodeName);
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
     * This is the template method for all clients of this class: in it clients will be given
     * an opportunity to configure a {@code proxy} object of the same type as you
     * want to configure. At least this way you get type-safety and ease of configuration.
     */
    protected abstract void configureXaConnectionFactory(DS xaDataSource);

    protected PoolingConnectionFactory buildXaConnectionFactory(
            String uniqueNodeName, Class<DS> xaDataSourceClassName)
            throws IllegalAccessException, InstantiationException {

        PoolingConnectionFactory poolingDataSource = new PoolingConnectionFactory();

        Map<String, Object> recordedProperties = new ConcurrentHashMap<String, Object>();
        DS recordingDataSource = PropertyRecordingProxyUtils.buildPropertyRecordingConnectionFactory(xaDataSourceClassName, recordedProperties);
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
}
