package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

public interface Message {

	byte[] protocolCode();

	int id();

	MessageType messageType();

	SerializationType serializationType();

	void serialize() throws SerializeException;

	void deserialize() throws DeserializeException;

}
