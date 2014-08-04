/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.util.Assert;

/**
 * Enumeration of common database drivers.
 *
 * @author Phillip Webb
 * @author Maciej Walkowiak
 * @since 1.2.0
 */
enum DatabaseDriver {

	/**
	 * Unknown type.
	 */
	UNKNOWN(null),

	/**
	 * Apache Derby.
	 */
	DERBY("org.apache.derby.jdbc.EmbeddedDriver"),

	/**
	 * H2.
	 */
	H2("org.h2.Driver"),

	/**
	 * HyperSQL DataBase.
	 */
	HSQLDB("org.hsqldb.jdbcDriver"),

	/**
	 * SQL Lite.
	 */
	SQLITE("org.sqlite.JDBC"),

	/**
	 * MySQL.
	 */
	MYSQL("com.mysql.jdbc.Driver"),

	/**
	 * Maria DB.
	 */
	MARIADB("org.mariadb.jdbc.Driver"),

	/**
	 * Google App Engine.
	 */
	GOOGLE("com.google.appengine.api.rdbms.AppEngineDriver"),

	/**
	 * Oracle
	 */
	ORACLE("oracle.jdbc.OracleDriver"),

	/**
	 * Postres
	 */
	POSTGRESQL("org.postgresql.Driver"),

	/**
	 * JTDS
	 */
	JTDS("net.sourceforge.jtds.jdbc.Driver"),

	/**
	 * SQL Server
	 */
	SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver");

	private final String driverClassName;

	private DatabaseDriver(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	/**
	 * @return the driverClassName
	 */
	public String getDriverClassName() {
		return this.driverClassName;
	}

	/**
	 * Find a {@link DatabaseDriver} for the given URL.
	 * @param jdbcUrl JDBC URL
	 * @return driver class name or {@link #UNKNOWN} if not found
	 */
	public static DatabaseDriver fromJdbcUrl(String jdbcUrl) {
		Assert.notNull(jdbcUrl, "JdbcUrl must not be null");
		Assert.isTrue(jdbcUrl.startsWith("jdbc"), "JdbcUrl must start with 'jdbc'");
		String urlWithoutPrefix = jdbcUrl.substring("jdbc".length()).toLowerCase();
		for (DatabaseDriver driver : values()) {
			String prefix = ":" + driver.name().toLowerCase() + ":";
			if (driver != UNKNOWN && urlWithoutPrefix.startsWith(prefix)) {
				return driver;
			}
		}
		return UNKNOWN;
	}

}
