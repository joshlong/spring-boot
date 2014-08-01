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

package sample.bitronix2;

import java.util.Map;

import javax.sql.DataSource;

import org.postgresql.xa.PGXADataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class HackingApplication {

	@Bean
	public PGXADataSource dataSource() {
		PGXADataSource xa = new PGXADataSource();
		xa.setServerName("127.0.0.1");
		xa.setDatabaseName("crm");
		xa.setUser("crm");
		xa.setPassword("crm");
		return xa;
	}

	@Bean
	public BitronixDataSourcePostProcessor postProcessor() {
		return new BitronixDataSourcePostProcessor();
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext run = SpringApplication.run(
				HackingApplication.class, args);
		Map<String, DataSource> bean = run.getBeansOfType(DataSource.class);
		System.out.println(bean);
	}

}
