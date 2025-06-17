package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.protocol.ProtocolCode;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

public interface Message {

	ProtocolCode protocolCode();

	int id();

	MessageType messageType();

	SerializationType serializationType();

	MessageHeader header();

	String payloadType();

	<T> T payload();

	void serialize() throws SerializeException;

	void deserialize() throws DeserializeException;

}
