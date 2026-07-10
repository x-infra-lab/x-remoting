package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.rpc.protocol.RemotingProtocolId;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

/**
 * response definition:
 * <p>
 * ｜getProtocol-codes:bytes|getProtocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|status:short|header-length:short|body-length:int|header:bytes|body:bytes|
 */

public class RemotingResponseMessage extends RemotingMessage implements ResponseMessage {

	private final MessageType messageType;

	final ResponseStatus status;

	private static final Integer protocolCodeLength = RemotingProtocolId.PROTOCOL_CODE.length;

	private static final Integer protocolVersionLength = Byte.BYTES;

	private static final Integer messageTypeLength = Byte.BYTES;

	private static final Integer requestIdLength = Integer.BYTES;

	private static final Integer serializationTypeLength = Byte.BYTES;

	private static final Integer statusLength = Short.BYTES;

	private static final Integer headerLengthLength = Short.BYTES;

	private static final Integer bodyLengthLength = Integer.BYTES;

	public static final Integer RESPONSE_HEADER_BYTES = protocolCodeLength + protocolVersionLength + messageTypeLength
			+ requestIdLength + serializationTypeLength + statusLength + headerLengthLength + bodyLengthLength;

	public RemotingResponseMessage(int id, SerializationType serializationType, ResponseStatus status) {
		this(id, MessageType.response, serializationType, status);
	}

	public RemotingResponseMessage(int id, MessageType messageType, SerializationType serializationType,
			ResponseStatus status) {
		super(id, serializationType);
		this.messageType = messageType;
		this.status = status;
	}

	@Override
	public MessageType getMessageType() {
		return messageType;
	}

	@Override
	public ResponseStatus getResponseStatus() {
		return status;
	}

}
