package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External configuration properties for a {@link org.springframework.transaction.jta.JtaTransactionManager}
 * created by Spring.
 *
 * @author Josh Long
 * @since 1.2
 */
@ConfigurationProperties(prefix = "spring.jta", ignoreUnknownFields = true)
public class JtaProperties {
}
