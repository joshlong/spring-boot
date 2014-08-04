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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link DatabaseDriver}.
 *
 * @author Phillip Webb
 * @author Maciej Walkowiak
 */
public class DatabaseDriverTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void classNameForKnownDatabase() {
		String driverClassName = DatabaseDriver.fromJdbcUrl(
				"jdbc:postgresql://hostname/dbname").getDriverClassName();
		assertEquals("org.postgresql.Driver", driverClassName);
	}

	@Test
	public void nullClassNameForUnknownDatabase() {
		String driverClassName = DatabaseDriver.fromJdbcUrl(
				"jdbc:unknowndb://hostname/dbname").getDriverClassName();
		assertNull(driverClassName);
	}

	@Test
	public void failureOnNullJdbcUrl() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("JdbcUrl must not be null");
		DatabaseDriver.fromJdbcUrl(null);
	}

	@Test
	public void failureOnMalformedJdbcUrl() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("JdbcUrl must start with");
		DatabaseDriver.fromJdbcUrl("malformed:url");
	}

}
