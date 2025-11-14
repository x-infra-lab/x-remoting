package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.serialization.SerializableObject;
import io.github.xinfra.lab.remoting.serialization.Serializer;

public interface MessageHeader extends SerializableObject {

	<T> void put(Key<T> key, T value);

	<T> T get(Key<T> key);

	boolean contains(Key<?> key);

	abstract class Key<T> {

		private final String name;

		private final Marshaller marshaller;

		public Key(String name, Marshaller marshaller) {
			this.name = name;
			this.marshaller = marshaller;
		}

		public static StringKey stringKey(String name) {
			return new StringKey(name);
		}

		public static <T> BinaryKey<T> binaryKey(String name, Serializer serializer, Class<T> clazz) {
			return new BinaryKey<>(name, serializer, clazz);
		}

	}

	class StringKey extends Key<String> {

		public StringKey(String name) {
			super(name, StringMarshaller.INSTANCE);
		}

	}

	class BinaryKey<T> extends Key<T> {

		public BinaryKey(String name, Serializer serializer, Class<T> clazz) {
			super(name, new BinaryMarshaller<>(serializer, clazz));
		}

	}

	interface Marshaller<T> {

		byte[] marshal(T value) throws SerializeException;

		T unmarshal(byte[] bytes) throws DeserializeException;

	}

	class StringMarshaller implements Marshaller<String> {

		public static final Marshaller<String> INSTANCE = new StringMarshaller();

		@Override
		public byte[] marshal(String value) {
			return value.getBytes();
		}

		@Override
		public String unmarshal(byte[] bytes) {
			return new String(bytes);
		}

	}

	class BinaryMarshaller<T> implements Marshaller<T> {

		private Serializer serializer;

		private Class<T> clazz;

		public BinaryMarshaller(Serializer serializer, Class<T> clazz) {
			this.serializer = serializer;
			this.clazz = clazz;
		}

		@Override
		public byte[] marshal(T value) throws SerializeException {
			return serializer.serialize(value);
		}

		@Override
		public T unmarshal(byte[] bytes) throws DeserializeException {
			return serializer.deserialize(bytes, clazz);
		}

	}

}
