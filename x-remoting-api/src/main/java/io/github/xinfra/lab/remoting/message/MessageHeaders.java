package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.serialization.SerializableObject;
import lombok.Getter;

public interface MessageHeaders extends SerializableObject {

	<T> void put(Key<T> key, T value);

	<T> T get(Key<T> key);

	boolean contains(Key<?> key);

	@Getter
	abstract class Key<T> {

		private final String name;

		private final Class<T> type;

		public Key(String name, Class<T> type) {
			this.name = name;
			this.type = type;
		}

		public static StringKey stringKey(String name) {
			return new StringKey(name);
		}

		public static <T> BinaryKey<T> binaryKey(String name, Class<T> clazz) {
			return new BinaryKey<>(name, clazz);
		}

	}

	class StringKey extends Key<String> {

		public StringKey(String name) {
			super(name, String.class);
		}

	}

	class BinaryKey<T> extends Key<T> {

		public BinaryKey(String name, Class<T> clazz) {
			super(name, clazz);
		}

	}

}
