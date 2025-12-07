package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.DefaultMessageHeaders;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.ProtocolId;
import io.github.xinfra.lab.remoting.impl.RemotingProtocolId;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.Setter;

public abstract class RemotingMessage implements Message {

	private int id;

	private SerializationType serializationType;

	@Setter
	private DefaultMessageHeaders headers;

	@Setter
	private RemotingMessageBody body;

	public RemotingMessage(int id, SerializationType serializationType) {
		this.id = id;
		this.serializationType = serializationType;
	}

	@Override
	public ProtocolId getProtocolIdentifier() {
		return RemotingProtocolId.INSTANCE;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public SerializationType getSerializationType() {
		return serializationType;
	}

	@Override
	public DefaultMessageHeaders getHeaders() {
		return headers;
	}

	@Override
	public RemotingMessageBody getBody() {
		return body;
	}

	@Override
	public void serialize() throws SerializeException {
		Serializer serializer = SerializationManager.getSerializer(serializationType);
		if (headers != null) {
			headers.serialize(serializer);
		}
		if (body != null) {
			body.serialize(serializer);
		}
	}

	@Override
	public void deserialize() throws DeserializeException {
		Serializer serializer = SerializationManager.getSerializer(serializationType);
		if (headers != null) {
			headers.deserialize(serializer);
		}
		if (body != null) {
			body.deserialize(serializer);
		}
	}

}
