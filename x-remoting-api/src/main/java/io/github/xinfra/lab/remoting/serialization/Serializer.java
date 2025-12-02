package io.github.xinfra.lab.remoting.serialization;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;

public interface Serializer {

	SerializationType getSerializationType();

	byte[] serialize(Object obj) throws SerializeException;

	<T> T deserialize(byte[] data, Class<T> clazz) throws DeserializeException;

}
