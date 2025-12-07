package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.impl.RemotingProtocolId;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

/**
 * response definition:
 * <p>
 * ｜getProtocol-codes:bytes|getProtocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|status:short|header-length:short|body-length:int|header:bytes|body:bytes|
 */

public class RemotingResponseMessage extends RemotingMessage implements ResponseMessage {

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
		super(id, serializationType);
		this.status = status;
	}

	@Override
	public ResponseStatus getResponseStatus() {
		return status;
	}

}
