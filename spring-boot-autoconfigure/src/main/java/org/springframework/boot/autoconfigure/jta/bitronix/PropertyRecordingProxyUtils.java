package org.springframework.boot.autoconfigure.jta.bitronix;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactoryBean;

import javax.jms.XAConnectionFactory;
import javax.sql.XADataSource;
import java.util.Map;

abstract class PropertyRecordingProxyUtils {

    public static <MQ extends XAConnectionFactory> MQ getPropertyRecordingConnectionFactory(
            final Class<MQ> clzz, final Map<String, Object> holderForProperties) {
        return getPropertyRecordingProxy(clzz, holderForProperties);
    }

    public static <DS extends XADataSource> DS getPropertyRecordingDataSource(
            final Class<DS> clzz, final Map<String, Object> holderForProperties) {
        return getPropertyRecordingProxy(clzz, holderForProperties);
    }

    private static void record(MethodInvocation methodInvocation, Map<String, Object> holder) {
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
    private static <T> T getPropertyRecordingProxy(final Class<T> clzz, final Map<String, Object> holderForProperties) {
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
