package org.springframework.boot.autoconfigure.jta.bitronix;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * Convenient base type for Bitronix resource registration.
 *
 * @param <XA_DRIVER>                XA specific subtype
 * @param <BITRONIX_POOLED_RESOURCE> pooled instance
 * @author Josh Long
 */
abstract class AbstractBitronixXaResourceFactoryBean<XA_DRIVER, BITRONIX_POOLED_RESOURCE>
        implements InitializingBean, BeanNameAware, FactoryBean<BITRONIX_POOLED_RESOURCE> {

    private Class<XA_DRIVER> xaDriverClass;
    private Class<BITRONIX_POOLED_RESOURCE> bitronixPooledResourceClass;

    private BITRONIX_POOLED_RESOURCE bitronixPooledResource;

    private String uniqueNodeName;

    public AbstractBitronixXaResourceFactoryBean(Class<XA_DRIVER> xaDriverClass, Class<BITRONIX_POOLED_RESOURCE> bitronixPooledResourceClass) {
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


    protected void record(MethodInvocation methodInvocation, Map<String, Object> holder) {
        String setterPrefix = "set";
        String methodInvName = methodInvocation.getMethod().getName();
        if (methodInvName.startsWith(setterPrefix)) {
            String propertyName = methodInvName.substring(setterPrefix.length());
            propertyName = Character.toLowerCase(propertyName.charAt(0)) +
                    propertyName.substring(1);
            Object[] args = methodInvocation.getArguments();
            if (args.length == 1) {
                holder.put(propertyName, args[0]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected  <T> T getPropertyRecordingProxy(final Class<T> clzz, final Map<String, Object> holderForProperties) {
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.setTargetClass(clzz);
        proxyFactoryBean.setProxyTargetClass(true);
        proxyFactoryBean.setAutodetectInterfaces(true);

        proxyFactoryBean.addAdvice(
                new MethodInterceptor() {
                    @Override
                    public Object invoke(MethodInvocation methodInvocation) throws Throwable {

                        record(methodInvocation, holderForProperties);

                        if (methodInvocation.getMethod().getName().equals("toString"))
                            return "a property recording proxy around a XA resource for Bitronix. " +
                                    "All calls to this proxy will be ignored and are only " +
                                    "to facilitate configuring a Bitronix-enlisted XA resource.";

                        return null;
                    }
                });
        return (T) proxyFactoryBean.getObject();
    }
}
