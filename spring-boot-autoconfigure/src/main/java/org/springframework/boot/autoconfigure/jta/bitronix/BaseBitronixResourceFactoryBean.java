package org.springframework.boot.autoconfigure.jta.bitronix;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Convenient base type for Bitronix resource registration.
 *
 * @param <XA_DRIVER>                XA specific subtype
 * @param <BITRONIX_POOLED_RESOURCE> pooled instance
 * @author Josh Long
 */
abstract class BaseBitronixResourceFactoryBean<XA_DRIVER, BITRONIX_POOLED_RESOURCE>
        implements InitializingBean, BeanNameAware, FactoryBean<BITRONIX_POOLED_RESOURCE> {

    private Class<XA_DRIVER> xaDriverClass;
    private Class<BITRONIX_POOLED_RESOURCE> bitronixPooledResourceClass;

    private BITRONIX_POOLED_RESOURCE bitronixPooledResource;

    private String uniqueNodeName;

    public BaseBitronixResourceFactoryBean(Class<XA_DRIVER> xaDriverClass, Class<BITRONIX_POOLED_RESOURCE> bitronixPooledResourceClass) {
        this.xaDriverClass = xaDriverClass;
        this.bitronixPooledResourceClass = bitronixPooledResourceClass;
        Assert.notNull(this.xaDriverClass, "you must provide a class type for the XA resource");
        Assert.notNull(this.bitronixPooledResourceClass, "you must provide a specific Bitronix resource subtype");
    }

    public Class<XA_DRIVER> getXaDriverClass() {
        return xaDriverClass;
    }

    @Override
    public void setBeanName(String name) {
        this.uniqueNodeName = name;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public BITRONIX_POOLED_RESOURCE getObject() throws Exception {
        return this.bitronixPooledResource;
    }

    @Override
    public Class<?> getObjectType() {
        return this.bitronixPooledResourceClass;
    }

    public String getUniqueNodeName() {
        return uniqueNodeName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.bitronixPooledResource = this.build();
    }

    /**
     * build the required transactional bitronixPooledResource delegating to the appropriate
     * Bitronix subtype (either {@link javax.jms.XAConnectionFactory} or
     * {@link javax.sql.XADataSource}.
     */
    public abstract BITRONIX_POOLED_RESOURCE build() throws Exception;

    /**
     * The callback hook that all clients must override. Configuration
     * made during this callback against the passed in proxy
     * will be <em>recorded</em> and passed to the Bitronix driver
     * as {@link java.util.Properties properties}.
     */
    protected abstract void configureXaResource(XA_DRIVER xa);

}

/*

public abstract class BitronixXaDataSourceFactoryBean<DS extends XADataSource>
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
*/
