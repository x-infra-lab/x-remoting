package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;

public class HeartbeatMessageTypeHandler implements MessageTypeHandler<RequestMessage> {

	@Override
	public MessageType messageType() {
		return MessageType.heartbeat;
	}

	@Override
	public void handleMessage(Connection connection, RequestMessage requestMessage) {
		MessageFactory messageFactory = connection.getProtocol().messageFactory();
		Responses.sendResponse(connection, messageFactory.createResponse(requestMessage.id(), ResponseStatus.OK));
	}

}
