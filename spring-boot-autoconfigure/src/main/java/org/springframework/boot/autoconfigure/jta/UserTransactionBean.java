package org.springframework.boot.autoconfigure.jta;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.Bean;

/**
 *
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD,
        java.lang.annotation.ElementType.METHOD,
        java.lang.annotation.ElementType.PARAMETER,
        java.lang.annotation.ElementType.TYPE,
        java.lang.annotation.ElementType.ANNOTATION_TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Inherited
@java.lang.annotation.Documented
@Qualifier(JtaAutoConfiguration.USER_TRANSACTION_NAME)
@Bean(name = JtaAutoConfiguration.USER_TRANSACTION_NAME)
public @interface UserTransactionBean {
    String value() default "";

    String initMethod() default "";

    String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

}
