package org.springframework.boot.autoconfigure.jta;

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
 *
 */
public class NarayanaDataSourceFactoryBean
        implements BeanNameAware,
        FactoryBean<DataSource> {

    private static Map<String, XADataSource> XA_DATA_SOURCE_MAP = new ConcurrentHashMap<String, XADataSource>();

    private String beanName;
    private final XADataSource xaDataSource;
    private String user, password;
    private Class<? extends DynamicClass> dynamicClass = SpringDynamicClass.class;

    public static class SpringDynamicClass implements DynamicClass {

        @Override
        public XADataSource getDataSource(String dataSourceBeanName) throws SQLException {
            return XA_DATA_SOURCE_MAP.get(dataSourceBeanName);
        }
    }

    public void setDynamicClass(Class<? extends DynamicClass> dynamicClass) {
        this.dynamicClass = dynamicClass;
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    public NarayanaDataSourceFactoryBean(
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
