package io.github.xinfra.lab.remoting.serialization;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClassFilterTest {

	@Test
	public void testBlockedClasses() {
		Assertions.assertFalse(ClassFilter.isAllowed("java.lang.Runtime"));
		Assertions.assertFalse(ClassFilter.isAllowed("java.lang.ProcessBuilder"));
		Assertions.assertFalse(ClassFilter.isAllowed("java.lang.Thread"));
		Assertions.assertFalse(ClassFilter.isAllowed("javax.management.MBeanServer"));
		Assertions.assertFalse(ClassFilter.isAllowed("com.sun.jmx.Something"));
		Assertions.assertFalse(ClassFilter.isAllowed("sun.misc.Unsafe"));
	}

	@Test
	public void testAllowedClasses() {
		Assertions.assertTrue(ClassFilter.isAllowed("java.lang.RuntimeException"));
		Assertions.assertTrue(ClassFilter.isAllowed("java.lang.String"));
		Assertions.assertTrue(ClassFilter.isAllowed("java.util.HashMap"));
	}

	@Test
	public void testLoadClassBlocked() {
		Assertions.assertThrows(DeserializeException.class, () -> ClassFilter.loadClass("java.lang.Runtime"));
	}

	@Test
	public void testLoadClassNotFound() {
		Assertions.assertThrows(DeserializeException.class, () -> ClassFilter.loadClass("com.nonexistent.FakeClass"));
	}

	@Test
	public void testLoadClassAllowed() throws DeserializeException {
		Class<?> clazz = ClassFilter.loadClass("java.lang.String");
		Assertions.assertEquals(String.class, clazz);
	}

	@Test
	public void testAddBlockedPrefix() {
		ClassFilter.addBlockedPrefix("com.custom.blocked.");
		Assertions.assertFalse(ClassFilter.isAllowed("com.custom.blocked.SomeClass"));
		Assertions.assertTrue(ClassFilter.isAllowed("com.custom.other.SomeClass"));
	}

}
