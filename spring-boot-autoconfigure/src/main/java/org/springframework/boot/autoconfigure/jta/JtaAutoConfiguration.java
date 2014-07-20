package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

import java.io.File;

/**
 * This auto-configuration registers JTA {@link javax.transaction.TransactionManager transactionManagers}
 * from various standalone JTA providers like <a href="http://www.atomikos.com">Atomikos</a>,
 * <A href="http://docs.codehaus.org/display/BTM/Home">Bitronix</A>, and
 * <A href="http://docs.jboss.org/jbosstm/">JBoss TM (a.k.a. Narayana, or Arjuna)</A>, if available.
 * <p>
 * This auto-configuration registers a {@link org.springframework.transaction.jta.JtaTransactionManager}
 * that uses a configured standalone JTA implementation or works with an application server's
 * JNDI-bound {@link javax.transaction.TransactionManager}.
 *
 * @author Josh Long
 */
@Configuration
@AutoConfigureBefore(DataSourceTransactionManagerAutoConfiguration.class)
@Conditional(JtaAutoConfiguration.JtaCondition.class)
@ConditionalOnClass(JtaTransactionManager.class)
public class JtaAutoConfiguration {


    /**
     * Are we running in an environment that has basic JTA-capable types on the CLASSPATH
     * <EM>and</EM> that has JTA-infrastructure beans, like a {@link javax.transaction.UserTransaction}
     * and a {@link javax.transaction.TransactionManager}.
     */
    public static class JtaCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ClassLoader classLoader = context.getClassLoader();

            // make sure we have both JTA transaction manager types of interest
            boolean jtaTransactionManagerTypesOnClassPath = ClassUtils.isPresent("javax.transaction.UserTransaction", classLoader) &&
                    ClassUtils.isPresent("javax.transaction.TransactionManager", classLoader);

            // and a type that can be used as a resource (such as an XADataSource or a XAConnectionFactory)
            boolean jtaResourceTypeOnClassPath = ClassUtils.isPresent("javax.jms.XAConnectionFactory", classLoader) ||
                    ClassUtils.isPresent("javax.sql.XADataSource", classLoader);

            if (jtaTransactionManagerTypesOnClassPath && jtaResourceTypeOnClassPath) {
                return ConditionOutcome.match();
            }

            return ConditionOutcome.noMatch("no XA class.");
        }
    }

    public static String jtaRootPathFor(ConfigurableEnvironment e, String jtaDistribution) {
        return e.getProperty("spring.jta." + jtaDistribution + ".rootPath",
                new File(System.getProperty("user.home"), "jta/" + jtaDistribution + "Data").getAbsolutePath());
    }

    @Order(2)
    @Configuration
    @Import(JndiAutoConfiguration.class)
    @Conditional(JtaCondition.class)
    @ConditionalOnMissingBean(name = "transactionManager", value = PlatformTransactionManager.class)
    @AutoConfigureAfter({ArjunaConfiguration.class, AtomikosConfiguration.class, BitronixConfiguration.class})
    public static class JndiConfiguration {
    }

    @Order(0)
    @Configuration
    @ConditionalOnClass(com.arjuna.ats.jta.UserTransaction.class)
    @Import(ArjunaAutoConfiguration.class)
    @ConditionalOnMissingBean(name = "transactionManager", value = PlatformTransactionManager.class)
    public static class ArjunaConfiguration {
    }

    @Order(0)
    @Configuration
    @Import(AtomikosAutoConfiguration.class)
    @ConditionalOnClass(com.atomikos.icatch.jta.UserTransactionManager.class)
    @ConditionalOnMissingBean(name = "transactionManager", value = PlatformTransactionManager.class)
    public static class AtomikosConfiguration {
    }

    @Order(0)
    @Configuration
    @Import(BitronixAutoConfiguration.class)
    @ConditionalOnClass(bitronix.tm.jndi.BitronixContext.class)
    @ConditionalOnMissingBean(name = "transactionManager", value = PlatformTransactionManager.class)
    public static class BitronixConfiguration {
    }

}