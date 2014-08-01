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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.ResolvableType;

/**
 * @author Phillip Webb
 */
public abstract class AbstractXaPostProcessor<S, D> implements
		BeanDefinitionRegistryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
			throws BeansException {
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;
		String[] beanNames = beanFactory.getBeanNamesForType(getSourceType(), false,
				false);
		for (String beanName : beanNames) {
			String sourceName = beanName + "XaSource";
			rename(registry, beanName, sourceName);
			BeanDefinition wrapper = createWrapperBean(registry, beanFactory, sourceName);
			registry.registerBeanDefinition(beanName, wrapper);
		}
	}

	private void rename(BeanDefinitionRegistry registry, String beanName,
			String sourceName) {
		BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
		registry.removeBeanDefinition(beanName);
		registry.registerBeanDefinition(sourceName, beanDefinition);
	}

	private BeanDefinition createWrapperBean(BeanDefinitionRegistry registry,
			ConfigurableListableBeanFactory beanFactory, String sourceName) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		beanDefinition.setBeanClass(XaWrapperFactoryBean.class);
		ConstructorArgumentValues constructorArgs = beanDefinition
				.getConstructorArgumentValues();
		RuntimeBeanReference source = new RuntimeBeanReference(sourceName);
		constructorArgs.addIndexedArgumentValue(0, beanFactory);
		constructorArgs.addIndexedArgumentValue(1, source);
		constructorArgs.addIndexedArgumentValue(2, this);
		return beanDefinition;
	}

	protected Class<?> getSourceType() {
		return ResolvableType.forClass(AbstractXaPostProcessor.class, getClass())
				.resolveGeneric(0);
	}

	protected Class<?> getDestinationType() {
		return ResolvableType.forClass(AbstractXaPostProcessor.class, getClass())
				.resolveGeneric(1);
	}

	protected abstract D adapt(S source);

	/**
	 * Factory Bean to actual adapt the source to the destination.
	 */
	static class XaWrapperFactoryBean<S, D> implements FactoryBean<D>, BeanNameAware {

		private final ConfigurableListableBeanFactory beanFactory;

		private final S source;

		private final AbstractXaPostProcessor<S, D> processor;

		private D instance;

		private String beanName;

		public XaWrapperFactoryBean(ConfigurableListableBeanFactory beanFactory,
				S source, AbstractXaPostProcessor<S, D> processor) {
			this.beanFactory = beanFactory;
			this.source = source;
			this.processor = processor;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public Class<?> getObjectType() {
			return this.processor.getDestinationType();
		}

		@Override
		public D getObject() throws Exception {
			if (this.instance == null) {
				synchronized (this) {
					this.instance = this.processor.adapt(this.source);
					this.beanFactory.initializeBean(this.instance, this.beanName);
				}
			}
			return this.instance;
		}

		@Override
		public void setBeanName(String name) {
			this.beanName = name;
		}

	}

}
