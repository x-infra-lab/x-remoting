package io.github.xinfra.lab.remoting.message;

public interface ResponseMessage extends Message {

	ResponseStatus responseStatus();

	default MessageType messageType() {
		return MessageType.response;
	}

}
