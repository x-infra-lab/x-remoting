package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageHeader;
import io.github.xinfra.lab.remoting.message.MessagePayload;
import io.github.xinfra.lab.remoting.protocol.ProtocolCode;
import io.github.xinfra.lab.remoting.rpc.RpcProtocolCode;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.github.xinfra.lab.remoting.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public abstract class RpcMessage implements Message {

	private int id;

	private RpcMessageType rpcMessageType;

	private SerializationType serializationType;

	private RpcMessageHeader header;


	public RpcMessage(int id, RpcMessageType rpcMessageType, SerializationType serializationType) {
		this.id = id;
		this.rpcMessageType = rpcMessageType;
		this.serializationType = serializationType;
	}

	@Override
	public ProtocolCode protocolCode() {
		return RpcProtocolCode.INSTANCE;
	}

	@Override
	public int id() {
		return id;
	}

	public SerializationType serializationType() {
		return serializationType;
	}

	public RpcMessageType messageType() {
		return rpcMessageType;
	}

	@Override
	public MessageHeader header() {
		// todo
		return null;
	}

	@Override
	public MessagePayload payload() {
		// todo
		return null;
	}

	@Override
	public void serialize() throws SerializeException {
		this.serializeContentType();
		this.serializeHeader();
		this.serializeContent();
	}

	public void serializeContentType() throws SerializeException {
		if (contentType != null) {
			byte[] bytes = contentType.getBytes(StandardCharsets.UTF_8);
			setContentTypeData(bytes);
		}
	}

	public void setContentTypeData(byte[] bytes) {
		if (bytes != null) {
			int length = bytes.length;
			if (length > Short.MAX_VALUE) {
				throw new RuntimeException("contentType length exceed maximum, len=" + length);
			}
			this.contentTypeLength = (short) length;
			this.contentTypeData = bytes;
		}
	}

	public void serializeHeader() throws SerializeException {
		if (header != null) {
			Serializer serializer = SerializationManager.getSerializer(serializationType);
			byte[] bytes = serializer.serialize(header);
			setHeaderData(bytes);
		}
	}

	public void setHeaderData(byte[] bytes) {
		if (bytes != null) {
			int length = bytes.length;
			if (length > Short.MAX_VALUE) {
				throw new RuntimeException("contentType length exceed maximum, len=" + length);
			}
			this.headerLength = (short) length;
			this.headerData = bytes;
		}
	}

	public void serializeContent() throws SerializeException {
		if (content != null) {
			Serializer serializer = SerializationManager.getSerializer(serializationType);
			byte[] bytes = serializer.serialize(content);
			setContentData(bytes);
		}
	}

	public void setContentData(byte[] bytes) {
		this.contentLength = bytes.length;
		this.contentData = bytes;
	}

	@Override
	public void deserialize() throws DeserializeException {
		deserializeContentType();
		deserializeHeader();
		deserializeContent();
	}

	public void deserialize(RpcDeserializeLevel level) throws DeserializeException {
		if (level.ordinal() == RpcDeserializeLevel.CONTENT_TYPE.ordinal()) {
			deserializeContentType();
		}
		else if (level.ordinal() == RpcDeserializeLevel.HEADER.ordinal()) {
			deserializeContentType();
			deserializeHeader();
		}
		else {
			deserialize();
		}
	}

	private void deserializeContent() throws DeserializeException {
		if (content == null && contentData != null) {
			Class<?> clazz = null;
			try {
				clazz = Class.forName(contentType);
			}
			catch (ClassNotFoundException e) {
				throw new DeserializeException(e);
			}
			this.content = SerializationManager.getSerializer(serializationType).deserialize(contentData, clazz);
		}
	}

	private void deserializeHeader() throws DeserializeException {
		if (header == null && headerData != null) {
			this.header = SerializationManager.getSerializer(serializationType)
				.deserialize(headerData, RpcMessageHeader.class);
		}
	}

	private void deserializeContentType() {
		if (contentType == null && contentTypeData != null) {
			this.contentType = new String(contentTypeData, StandardCharsets.UTF_8);
		}
	}

}
