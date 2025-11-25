package io.github.xinfra.lab.remoting.serialization;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;

public interface SerializableObject {

	void serialize(Serializer serializer) throws SerializeException;

	void deserialize(Serializer serializer) throws DeserializeException;

	byte[] data();

}
