/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResetMocksTestExecutionListener}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ResetMocksTestExecutionListenerTests {

	static boolean test001executed;

	@Autowired
	private ApplicationContext context;

	@Test
	public void test001() {
		given(getMock("none").greeting()).willReturn("none");
		given(getMock("before").greeting()).willReturn("before");
		given(getMock("after").greeting()).willReturn("after");
		test001executed = true;
	}

	@Test
	public void test002() {
		assertThat(getMock("none").greeting()).isEqualTo("none");
		assertThat(getMock("before").greeting()).isNull();
		assertThat(getMock("after").greeting()).isNull();
	}

	public ExampleService getMock(String name) {
		return this.context.getBean(name, ExampleService.class);
	}

	@Configuration
	static class Config {

		@Bean
		public ExampleService before() {
			return mock(ExampleService.class, MockReset.before());
		}

		@Bean
		public ExampleService after() {
			return mock(ExampleService.class, MockReset.before());
		}

		@Bean
		public ExampleService none() {
			return mock(ExampleService.class);
		}

	}

}
