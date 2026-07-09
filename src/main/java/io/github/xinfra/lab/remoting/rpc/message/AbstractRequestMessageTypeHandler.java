package io.github.xinfra.lab.remoting.rpc.message;

public abstract class AbstractRequestMessageTypeHandler implements MessageTypeHandler<RequestMessage> {

	@Override
	public MessageType getMessageType() {
		return MessageType.request;
	}

}
