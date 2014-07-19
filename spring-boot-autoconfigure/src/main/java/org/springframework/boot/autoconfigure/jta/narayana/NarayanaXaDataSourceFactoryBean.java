package org.springframework.boot.autoconfigure.jta.narayana;

import com.arjuna.ats.internal.jdbc.DynamicClass;
import com.arjuna.ats.jdbc.TransactionalDriver;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * wraps the {@link com.arjuna.ats.jdbc.TransactionalDriver} implementation that JBoss TM already ships with
 * as a generic, XA-aware {@link javax.sql.DataSource}.
 *
 * @author Josh Long
 */
public class NarayanaXaDataSourceFactoryBean
        implements BeanNameAware, FactoryBean<DataSource> {

    private static Map<String, XADataSource> XA_DATA_SOURCE_MAP = new ConcurrentHashMap<String, XADataSource>();

    /**
     * This will be passed into the {@link com.arjuna.ats.internal.jdbc.DynamicClass} <em>driver</em>
     */
    private String beanName;

    /**
     * The target {@link javax.sql.XADataSource} to which calls should be routed
     */
    private final XADataSource xaDataSource;

    /**
     * passed to the {@link com.arjuna.ats.jdbc.TransactionalDriver}
     */
    private String user;

    /**
     * passed to the {@link com.arjuna.ats.jdbc.TransactionalDriver}
     */
    private String password;

    /**
     * bit of indirection to lookup a {@link javax.sql.DataSource}
     */
    private Class<? extends DynamicClass> dynamicClass = SpringDynamicClass.class;

    /**
     * Provides access to the original {@link javax.sql.DataSource}. I'm not sure why we
     * need to provide this instead of a simple {@link javax.sql.DataSource} reference, but here it is.
     * <p>
     * We store all {@link javax.sql.DataSource} beans in the {@link #XA_DATA_SOURCE_MAP} map
     * and key them by <code>beanName</code>. That  <code>beanName</code> is then
     * passed as a property into the {@link com.arjuna.ats.jdbc.TransactionalDriver}
     * which then looks the bean up by asking the {@link com.arjuna.ats.internal.jdbc.DynamicClass}
     * implementation. In this case, that implementation just looks the bean up in the map.
     */
    public static class SpringDynamicClass implements DynamicClass {

        @Override
        public XADataSource getDataSource(String dataSourceBeanName) throws SQLException {
            return XA_DATA_SOURCE_MAP.get(dataSourceBeanName);
        }
    }

    public void setDynamicClass(Class<? extends DynamicClass> dynamicClass) {
        this.dynamicClass = dynamicClass;
    }

    /**
     * Used to map our target {@link javax.sql.DataSource}
     */
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    public NarayanaXaDataSourceFactoryBean(
            XADataSource xaDataSource, String username, String password) {
        this.xaDataSource = xaDataSource;
        this.user = username;
        this.password = password;
    }

    @Override
    public DataSource getObject() throws Exception {
        XA_DATA_SOURCE_MAP.put(beanName, xaDataSource);
        TransactionalDriver transactionalDriver = new TransactionalDriver();
        Properties properties = new Properties();
        properties.setProperty(TransactionalDriver.userName, this.user);
        properties.setProperty(TransactionalDriver.password, this.password);
        properties.setProperty(TransactionalDriver.dynamicClass, dynamicClass.getName());
        String url = TransactionalDriver.arjunaDriver + "" + beanName;
        SimpleDriverDataSource simpleDriverDataSource =
                new SimpleDriverDataSource(transactionalDriver, url, properties);
        simpleDriverDataSource.setUsername(this.user);
        simpleDriverDataSource.setPassword(this.password);
        return simpleDriverDataSource;
    }

    @Override
    public Class<?> getObjectType() {
        return DataSource.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
