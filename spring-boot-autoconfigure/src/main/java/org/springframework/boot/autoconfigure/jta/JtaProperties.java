package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * External configuration properties for a {@link JtaTransactionManager} created by
 * Spring.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = JtaProperties.PREFIX, ignoreUnknownFields = true)
public class JtaProperties {

	public static final String PREFIX = "spring.jta";

	// FIXME folder

}
