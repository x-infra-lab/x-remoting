package io.github.xinfra.lab.remoting.message;

public abstract class AbstractRequestMessageTypeHandler implements MessageTypeHandler<RequestMessage> {

	@Override
	public MessageType getMessageType() {
		return MessageType.request;
	}

}
