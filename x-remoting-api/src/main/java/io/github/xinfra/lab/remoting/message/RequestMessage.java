package io.github.xinfra.lab.remoting.message;

public interface RequestMessage extends Message {

	String path();

	@Override
	default MessageType messageType() {
		return MessageType.request;
	}

}
