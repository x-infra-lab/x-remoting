package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.protocol.ProtocolIdentifier;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

public interface Message {

	ProtocolIdentifier protocolIdentifier();

	int id();

	SerializationType serializationType();

	MessageType messageType();

	MessageHeader header();

	MessageBody body();

	void serialize() throws SerializeException;

	void deserialize() throws DeserializeException;

}
