package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * request definition:
 * <p>
 * ｜protocol:bytes|message-type:byte|request-id:int|serialization-type:byte|content-type-length:short|header-length:short|content-length:int|content-type|header|content|
 * <p>
 */

@Setter
@Getter
@ToString
public class RpcRequestMessage extends RpcMessage {

	public RpcRequestMessage(int id) {
		this(id, SerializationType.HESSION);
	}

	public RpcRequestMessage(int id, SerializationType serializationType) {
		super(id, MessageType.request, serializationType);
	}

	public RpcRequestMessage(int id, MessageType messageType, SerializationType serializationType) {
		super(id, messageType, serializationType);
	}

}
