package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.serialization.SerializableObject;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.Getter;

public interface MessageHeaders extends SerializableObject {

    <T> void put(Key<T> key, T value);

    <T> T get(Key<T> key);

    boolean contains(Key<?> key);


    @Getter
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

        public static <T> BinaryKey<T> binaryKey(String name, SerializationType serializationType, Class<T> clazz) {
            return new BinaryKey<>(name, serializationType, clazz);
        }


    }

    class StringKey extends Key<String> {

        public StringKey(String name) {
            super(name, StringMarshaller.INSTANCE);
        }

    }

    class BinaryKey<T> extends Key<T> {

        public BinaryKey(String name, SerializationType serializationType, Class<T> clazz) {
            super(name, new BinaryMarshaller<>(SerializationManager.getSerializer(serializationType), clazz));
        }

    }

    interface Marshaller<T> {

        Class<T> type();

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

        @Override
        public Class<String> type() {
            return String.class;
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

        @Override
        public Class<T> type() {
            return clazz;
        }
    }

}
