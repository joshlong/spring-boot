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

package org.springframework.boot.autoconfigure.jms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.hornetq.HornetQAutoConfiguration;
import org.springframework.boot.autoconfigure.jta.JtaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.jms.ConnectionFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring JMS.
 *
 * @author Greg Turnquist
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass(JmsTemplate.class)
@ConditionalOnBean(ConnectionFactory.class)
@EnableConfigurationProperties(JmsProperties.class)
@AutoConfigureAfter({JtaAutoConfiguration.class, HornetQAutoConfiguration.class, ActiveMQAutoConfiguration.class})
public class JmsAutoConfiguration {

    @Autowired
    private JmsProperties properties;

    @Autowired
    private ConnectionFactory connectionFactory;

    protected JmsTemplate buildJmsTemplate() {
        JmsTemplate jmsTemplate = new JmsTemplate(this.connectionFactory);
        jmsTemplate.setPubSubDomain(this.properties.isPubSubDomain());
        boolean sessionTransacted = this.properties.isSessionTransacted();
        if (sessionTransacted) {
            jmsTemplate.setSessionTransacted(sessionTransacted);
            jmsTemplate.setSessionAcknowledgeMode(javax.jms.Session.CLIENT_ACKNOWLEDGE);
        }
        return jmsTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public JmsTemplate jmsTemplate() {
        return buildJmsTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(JtaTransactionManager.class)
    public JmsTemplate jmsTemplate(JtaTransactionManager jtaTransactionManager) {
        return buildJmsTemplate();
    }

}
