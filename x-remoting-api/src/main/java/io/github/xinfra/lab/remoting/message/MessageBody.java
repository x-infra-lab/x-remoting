package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.serialization.Serializer;

public interface MessageBody {

	byte[] serialize(Serializer serializer) throws SerializeException;

	void deserialize(Serializer serializer, byte[] bodyData) throws DeserializeException;

}
