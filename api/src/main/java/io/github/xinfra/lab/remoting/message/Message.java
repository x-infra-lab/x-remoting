package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.protocol.ProtocolId;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

public interface Message {

	ProtocolId getProtocolIdentifier();

	int getId();

	SerializationType getSerializationType();

	MessageType getMessageType();

	MessageHeaders getHeaders();

	MessageBody getBody();

	void serialize() throws SerializeException;

	void deserialize() throws DeserializeException;

}
