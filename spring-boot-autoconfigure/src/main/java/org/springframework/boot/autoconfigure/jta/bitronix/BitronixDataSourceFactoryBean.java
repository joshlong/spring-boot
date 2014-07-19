package org.springframework.boot.autoconfigure.jta.bitronix;

import bitronix.tm.resource.jdbc.PoolingDataSource;

import javax.sql.XADataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers a Bitronix {@link  bitronix.tm.resource.jdbc.PoolingDataSource} that will participate
 * in Bitronix-managed XA transactions.
 *
 * @param <XA> the type of the {@link javax.sql.XADataSource} subclass that you want to use.
 * @author Josh Long
 */
public abstract class BitronixDataSourceFactoryBean<XA extends XADataSource> extends BaseBitronixResourceFactoryBean<XA, PoolingDataSource> {

    public BitronixDataSourceFactoryBean(Class<XA> xaDataSourceClass) {
        super(xaDataSourceClass , PoolingDataSource.class);
    }

    @Override
    public PoolingDataSource build() throws Exception {

        PoolingDataSource ds = new PoolingDataSource();
        Map<String, Object> recordedProperties = new ConcurrentHashMap<String, Object>();
        XA recordingDataSource = PropertyRecordingProxyUtils.getPropertyRecordingDataSource(this.getXaDriverClass(), recordedProperties);
        this.configureXaResource(recordingDataSource);

        ds.setClassName(getXaDriverClass().getName());
        ds.setMaxPoolSize(10);
        ds.setAllowLocalTransactions(true);
        ds.setEnableJdbc4ConnectionTest(true);
        ds.getDriverProperties().putAll(recordedProperties);
        ds.setUniqueName(getUniqueNodeName());
        ds.init();
        return ds;
    }
}
