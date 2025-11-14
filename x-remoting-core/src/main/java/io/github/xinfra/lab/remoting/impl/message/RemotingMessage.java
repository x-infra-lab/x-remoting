package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.ProtocolIdentifier;
import io.github.xinfra.lab.remoting.impl.RemotingProtocolIdentifier;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.Setter;

public abstract class RemotingMessage implements Message {

	private int id;

	private SerializationType serializationType;

	@Setter
	private RemotingMessageHeader header;

	@Setter
	private RemotingMessageBody body;

	public RemotingMessage(int id, SerializationType serializationType) {
		this.id = id;
		this.serializationType = serializationType;
	}

	@Override
	public ProtocolIdentifier protocolIdentifier() {
		return RemotingProtocolIdentifier.INSTANCE;
	}

	@Override
	public int id() {
		return id;
	}

	public SerializationType serializationType() {
		return serializationType;
	}

	@Override
	public RemotingMessageHeader header() {
		return header;
	}

	@Override
	public RemotingMessageBody body() {
		return body;
	}

	@Override
	public void serialize() throws SerializeException {
		Serializer serializer = SerializationManager.getSerializer(serializationType);
		if (header != null) {
			header.serialize(serializer);
		}
		if (body != null) {
			body.serialize(serializer);
		}
	}

	@Override
	public void deserialize() throws DeserializeException {
		Serializer serializer = SerializationManager.getSerializer(serializationType);
		if (header != null) {
			header.deserialize(serializer);
		}
		if (body != null) {
			body.deserialize(serializer);
		}
	}

}
