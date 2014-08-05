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

package org.springframework.boot.jta.bitronix;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.boot.jta.XADataSourceWrapper;

/**
 * {@link XADataSourceWrapper} that uses a {@link PoolingDataSourceBean} to wrap a
 * {@link XADataSource}.
 *
 * @author Phillip Webb
 */
public class PollingDataSourceXAWrapper implements XADataSourceWrapper, BeanNameAware {

	private String beanName;

	@Override
	public DataSource wrapDataSource(XADataSource dataSource) throws Exception {
		PoolingDataSourceBean pool = new PoolingDataSourceBean();
		pool.setBeanName(this.beanName);
		pool.setDataSource(dataSource);
		customize(pool);
		pool.afterPropertiesSet();
		pool.init();
		return pool;
	}

	protected void customize(PoolingDataSourceBean pool) {
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

}
