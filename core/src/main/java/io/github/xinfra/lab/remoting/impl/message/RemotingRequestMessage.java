package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocolId;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

/**
 * request definition:
 * <p>
 * ｜getProtocol-codeRemotingRequestMessages:bytes|getProtocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|path-length:short|header-length:short|body-length:int|path:bytes|header:bytes|body:bytes|
 */

public class RemotingRequestMessage extends RemotingMessage implements RequestMessage {

	@Setter
	@Getter
	private String path;

	@Setter
	@Getter
	private byte[] pathData;

	private MessageType messageType;

	private boolean serialized;

	private boolean deserialized;

	private static final Integer protocolCodeLength = RemotingProtocolId.PROTOCOL_CODE.length;

	private static final Integer protocolVersionLength = Byte.BYTES;

	private static final Integer messageTypeLength = Byte.BYTES;

	private static final Integer requestIdLength = Integer.BYTES;

	private static final Integer serializationTypeLength = Byte.BYTES;

	private static final Integer pathLengthLength = Short.BYTES;

	private static final Integer headerLengthLength = Short.BYTES;

	private static final Integer bodyLengthLength = Integer.BYTES;

	public static final Integer REQUEST_HEADER_BYTES = protocolCodeLength + protocolVersionLength + messageTypeLength
			+ requestIdLength + serializationTypeLength + pathLengthLength + headerLengthLength + bodyLengthLength;

	public RemotingRequestMessage(int id, MessageType messageType, SerializationType serializationType) {
		super(id, serializationType);
		this.messageType = messageType;
	}

	@Override
	public MessageType getMessageType() {
		return messageType;
	}

	@Override
	public void serialize() throws SerializeException {
		super.serialize();
		if (!serialized) {
			serialized = true;
			if (path == null) {
				pathData = new byte[0]; // default: ""
			}
			else {
				pathData = path.getBytes(StandardCharsets.UTF_8);
			}
		}
	}

	@Override
	public void deserialize() throws DeserializeException {
		super.deserialize();
		if (!deserialized) {
			deserialized = true;
			if (pathData == null) {
				return;
			}
			path = new String(pathData, StandardCharsets.UTF_8);
		}
	}

}
