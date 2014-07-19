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
 */@Deprecated
  abstract class BitronixXaDataSourceFactoryBean<DS extends XADataSource>
        implements InitializingBean, BeanNameAware, FactoryBean<PoolingDataSource> {

    private final Class<DS> xaDataSourceClass;
    private String uniqueNodeName;

    public BitronixXaDataSourceFactoryBean(Class<DS> xaDataSourceClass) {
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

    protected abstract void configureXaDataSource(DS xaDataSource);

    protected PoolingDataSource buildPoolingDataSource(
            String uniqueNodeName, Class<DS> xaDataSourceClassName) {
        PoolingDataSource ds = new PoolingDataSource();
        Map<String, Object> recordedProperties = new ConcurrentHashMap<String, Object>();
        DS recordingDataSource = PropertyRecordingProxyUtils.getPropertyRecordingDataSource(this.xaDataSourceClass, recordedProperties);

        this.configureXaDataSource(recordingDataSource);

        ds.setClassName(xaDataSourceClassName.getName());
        ds.setMaxPoolSize(10);
        ds.setAllowLocalTransactions(true);
        ds.setEnableJdbc4ConnectionTest(true);
        ds.getDriverProperties().putAll(recordedProperties);
        ds.setUniqueName(uniqueNodeName);
        ds.init();
        return ds;
    }

    private PoolingDataSource poolingDataSource;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.poolingDataSource = this.buildPoolingDataSource(
                this.uniqueNodeName, xaDataSourceClass);
    }
}
