package io.github.xinfra.lab.remoting.serialization;

import io.github.xinfra.lab.remoting.exception.DeserializeException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ClassFilter {

	private static final List<String> BLOCKED_PREFIXES = Collections.unmodifiableList(Arrays.asList("javax.management.",
			"com.sun.jmx.", "com.sun.org.apache.", "org.apache.xbean.", "org.apache.commons.collections.functors.",
			"org.apache.commons.collections4.functors.", "org.codehaus.groovy.runtime.", "org.springframework.beans.",
			"javassist.", "bsh.", "org.mozilla.javascript.", "com.mchange.", "org.apache.bcel.",
			"org.apache.commons.beanutils.", "java.lang.reflect.Proxy", "sun.misc.Unsafe", "sun.reflect.",
			"net.sf.cglib.", "com.alibaba.fastjson."));

	private static final List<String> BLOCKED_CLASSES = Collections
		.unmodifiableList(Arrays.asList("java.lang.Runtime", "java.lang.ProcessBuilder", "java.lang.Thread"));

	private static final Set<String> extraBlockedPrefixes = new CopyOnWriteArraySet<>();

	public static void addBlockedPrefix(String prefix) {
		extraBlockedPrefixes.add(prefix);
	}

	public static boolean isAllowed(String className) {
		for (String blocked : BLOCKED_CLASSES) {
			if (className.equals(blocked)) {
				return false;
			}
		}
		for (String prefix : BLOCKED_PREFIXES) {
			if (className.startsWith(prefix)) {
				return false;
			}
		}
		for (String prefix : extraBlockedPrefixes) {
			if (className.startsWith(prefix)) {
				return false;
			}
		}
		return true;
	}

	public static Class<?> loadClass(String className) throws DeserializeException {
		if (!isAllowed(className)) {
			throw new DeserializeException("Deserialization of class is not allowed: " + className);
		}
		try {
			return Class.forName(className);
		}
		catch (ClassNotFoundException e) {
			throw new DeserializeException("Class not found: " + className, e);
		}
	}

}
