package org.springframework.boot.autoconfigure.jta;

import org.springframework.beans.factory.annotation.Qualifier;
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
@Bean(name = JtaAutoConfiguration.TRANSACTION_MANAGER_NAME)
@Qualifier(JtaAutoConfiguration.TRANSACTION_MANAGER_NAME)
public @interface TransactionManagerBean {
    String value() default "";
}
