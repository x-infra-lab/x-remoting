package io.github.xinfra.lab.remoting.message;

public interface ResponseMessage extends Message {

	ResponseStatus getResponseStatus();

	default MessageType getMessageType() {
		return MessageType.response;
	}

}
