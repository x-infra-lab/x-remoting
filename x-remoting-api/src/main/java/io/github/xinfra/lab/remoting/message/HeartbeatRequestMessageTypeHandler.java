package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;

public class HeartbeatRequestMessageTypeHandler implements MessageTypeHandler<RequestMessage> {

	@Override
	public MessageType getMessageType() {
		return MessageType.heartbeatRequest;
	}

	@Override
	public void handleMessage(Connection connection, RequestMessage requestMessage) {
		ResponseMessage responseMessage = connection.getProtocol()
			.getMessageFactory()
			.createResponse(requestMessage.getId(), requestMessage.getSerializationType(), ResponseStatus.OK);
		Responses.sendResponse(connection, responseMessage);
	}

}
