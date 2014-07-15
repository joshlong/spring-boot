package org.springframework.boot.autoconfigure.jta.bitronix;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.XADataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bitronix provides a {@link bitronix.tm.resource.jdbc.PoolingDataSource} that
 * wants to configure the {@link javax.sql.XADataSource} for you. This {@link org.springframework.beans.factory.FactoryBean}
 * lets you configure a proxy (and thus take advantage of type safety) and supports configuration options
 * for the {@link bitronix.tm.resource.jdbc.PoolingDataSource} that's ultimately returned.
 *
 * @author Josh Long
 */
public abstract class BitronixXaDataSourceFactoryBean<T extends XADataSource>
        implements InitializingBean, BeanNameAware, FactoryBean<PoolingDataSource> {

    private final Class<T> xaDataSourceClass;
    private String uniqueNodeName;

    public BitronixXaDataSourceFactoryBean(Class<T> xaDataSourceClass) {
        this.xaDataSourceClass = xaDataSourceClass;
    }

    @Override
    public PoolingDataSource getObject() throws Exception {
        return this.poolingDataSource;
    }

    @Override
    public Class<?> getObjectType() {
        return PoolingDataSource.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setBeanName(String name) {
        this.uniqueNodeName = name + Long.toString(System.currentTimeMillis());
    }

    protected PoolingDataSource buildXaDataSource(
            String uniqueNodeName, Class<T> xaDataSourceClassName)
            throws IllegalAccessException, InstantiationException {

        PoolingDataSource poolingDataSource = new PoolingDataSource();

        Map<String, Object> recordedProperties = new ConcurrentHashMap<String, Object>();
        T recordingDataSource = PropertyRecordingProxyUtils.buildPropertyRecordingXaDataSource(
                xaDataSourceClassName, recordedProperties);
        configureXaDataSource(recordingDataSource);

        poolingDataSource.setClassName(xaDataSourceClassName.getName());
        poolingDataSource.setMaxPoolSize(10);
        poolingDataSource.getDriverProperties().putAll(recordedProperties);
        poolingDataSource.setUniqueName(uniqueNodeName);
        poolingDataSource.init();

        return poolingDataSource;
    }

    protected abstract void configureXaDataSource(T xaDataSource);

    private PoolingDataSource poolingDataSource;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.poolingDataSource = this.buildXaDataSource(this.uniqueNodeName, this.xaDataSourceClass);
    }
}
