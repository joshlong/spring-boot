package org.springframework.boot.autoconfigure.jta;


/**
 * Tests the JTA support.
 *
 * @author Josh Long
 */
public class JtaAutoConfigurationTests {
	// FIXME
	//
	// private AnnotationConfigApplicationContext applicationContext;
	//
	// @After
	// public void close() {
	// if (null != this.applicationContext) {
	// this.applicationContext.close();
	// }
	// }
	//
	// @Test
	// public void testAtomikosAutoConfiguration() throws Exception {
	// testContextFor(AtomikosJtaAutoConfiguration.class, UserTransactionManager.class);
	// }
	//
	// @Test
	// public void testBitronixAutoConfiguration() throws Exception {
	// testContextFor(BitronixJtaAutoConfiguration.class,
	// BitronixTransactionManager.class);
	// }
	//
	// private AnnotationConfigApplicationContext buildApplicationContextFrom(
	// Class<?>... classes) {
	// AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
	// ac.register(classes);
	// ac.refresh();
	// this.applicationContext = ac;
	// return ac;
	// }
	//
	// private AnnotationConfigApplicationContext testContextFor(
	// Class<?> classOfAutoConfiguration, Class<?> classOfTransactionManagerImpl) {
	// AnnotationConfigApplicationContext ac = this
	// .buildApplicationContextFrom(classOfAutoConfiguration);
	// JtaTransactionManager jtaTransactionManager = ac
	// .getBean(JtaTransactionManager.class);
	// Assert.notNull(jtaTransactionManager, "the transactionManager should not be null");
	// Assert.isAssignable(classOfTransactionManagerImpl, jtaTransactionManager
	// .getTransactionManager().getClass(), "expecting a subclass of type "
	// + classOfTransactionManagerImpl.getName());
	// return ac;
	// }

}
