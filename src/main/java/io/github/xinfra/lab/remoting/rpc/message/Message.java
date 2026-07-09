package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.protocol.ProtocolId;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

public interface Message extends io.github.xinfra.lab.remoting.message.Message {

	int getId();

	ProtocolId getProtocolIdentifier();

	SerializationType getSerializationType();

	MessageType getMessageType();

	MessageHeaders getHeaders();

	MessageBody getBody();

	void serialize() throws SerializeException;

	void deserialize() throws DeserializeException;

}
