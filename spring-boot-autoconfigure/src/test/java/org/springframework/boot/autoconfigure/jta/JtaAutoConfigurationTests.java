package org.springframework.boot.autoconfigure.jta;

import bitronix.tm.BitronixTransactionManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.atomikos.icatch.jta.UserTransactionManager;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;

/**
 * Tests the JTA support.
 *
 * @author Josh Long
 */
public class JtaAutoConfigurationTests {

    private AnnotationConfigApplicationContext applicationContext;

    @After
    public void close() {
        if (null != this.applicationContext)
            this.applicationContext.close();
    }

    @Test
    public void testAtomikosAutoConfiguration() throws Exception {
        testContextFor(AtomikosAutoConfiguration.class, UserTransactionManager.class);
    }

    @Test
    public void testBitronixAutoConfiguration() throws Exception {
        testContextFor(BitronixAutoConfiguration.class, BitronixTransactionManager.class);
    }

    @Test
    public void testNarayanaAutoConfiguration() throws Exception {
        AnnotationConfigApplicationContext ac = testContextFor(
                NarayanaAutoConfiguration.class,
                TransactionManagerImple.class);

    }

    private AnnotationConfigApplicationContext buildApplicationContextFrom(Class<?>... classes) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext
                = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(classes);
        annotationConfigApplicationContext.refresh();
        this.applicationContext = annotationConfigApplicationContext;
        return annotationConfigApplicationContext;
    }

    private AnnotationConfigApplicationContext testContextFor(Class<?> classOfAutoConfiguration, Class<?> classOfTransactionManagerImpl) {
        AnnotationConfigApplicationContext ac = this.buildApplicationContextFrom(classOfAutoConfiguration);
        JtaTransactionManager jtaTransactionManager = ac.getBean(JtaTransactionManager.class);
        Assert.notNull(jtaTransactionManager, "the transactionManager should not be null");
        Assert.isAssignable(classOfTransactionManagerImpl, jtaTransactionManager.getTransactionManager().getClass(),
                "expecting a subclass of type " + classOfTransactionManagerImpl.getName());
        Assert.isTrue(
                SpringJtaPlatform.JTA_TRANSACTION_MANAGER.get() == jtaTransactionManager,
                "the configured JtaPlatform must have the same reference as the Spring context");
        return ac;
    }


}
