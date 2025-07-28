package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * response definition:
 * <p>
 * ｜protocol:bytes|protocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|status:short|header-length:short|body-length:int|header|content|
 */

@Setter
@Getter
@ToString
public class RemotingResponseMessage extends RemotingMessage implements ResponseMessage {

	final ResponseStatus status;

	public RemotingResponseMessage(int id, MessageType messageType, SerializationType serializationType,
								   ResponseStatus status) {
		super(id, messageType, serializationType);
        this.status = status;
    }

	@Override
	public ResponseStatus status() {
		return status;
	}
}
