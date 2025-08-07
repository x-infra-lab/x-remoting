package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;

public abstract class RequestMessageTypeHandler implements MessageTypeHandler<RequestMessage> {

	@Override
	public MessageType messageType() {
		return MessageType.request;
	}

	@Override
	public void handleMessage(Connection connection, RequestMessage requestMessage) {
		ResponseMessage responseMessage = handleRequestMessage(requestMessage);
		Responses.sendResponse(connection, responseMessage);
	}

	public abstract ResponseMessage handleRequestMessage(RequestMessage requestMessage);


}
