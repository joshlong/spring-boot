package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.sql.DataSource;

/**
 * Attempts to register JTA transaction managers.
 * <p> Will support several strategies. Thinking aloud, these might include:
 * <p>
 * <OL>
 * <LI> Atomikos & (Tomcat || Jetty) </li>
 * <LI> BTM & (Tomcat || Jetty) </li>
 * <LI> JOTM & (Tomcat || Jetty)</li>
 * <LI> Narayana & (Tomcat || Jetty) </li>
 * <li>Standard Application server JTA search strategy as supported directly
 * by {@link org.springframework.transaction.jta.JtaTransactionManager}.</li>
 * </OL>
 * <p>
 * <p>
 * For a start, Spring Boot will try to pull well-known transactional resources in a
 * a given bean container.
 *
 * @author Josh Long
 */
@Configuration
@AutoConfigureAfter(DataSourceTransactionManagerAutoConfiguration.class)
@ConditionalOnClass(JtaTransactionManager.class)
public class JtaTransactionManagerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "transactionManager")
    @ConditionalOnBean(DataSource.class)
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }




}
