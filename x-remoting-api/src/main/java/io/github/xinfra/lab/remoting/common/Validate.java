package io.github.xinfra.lab.remoting.common;

import java.util.Objects;

public class Validate {

	public static void isTrue(final boolean expression, final String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void notNull(final Object obj, final String message) {
		Objects.requireNonNull(obj, message);
	}

	public static void inclusiveBetween(long start, long end, long value, String message) {
		if (value < start || value > end) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void isTrue(boolean expression, String message, Object... values) {
		if (!expression) {
			throw new IllegalArgumentException(String.format(message, values));
		}
	}

	public static void notBlank(final String str, final String message) {
		Objects.requireNonNull(str, message);
		if (str.trim().isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

}
