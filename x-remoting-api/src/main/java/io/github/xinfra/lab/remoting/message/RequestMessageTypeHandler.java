package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.common.Requests;
import io.github.xinfra.lab.remoting.connection.Connection;

public abstract class RequestMessageTypeHandler implements MessageTypeHandler<RequestMessage> {

	@Override
	public MessageType messageType() {
		return MessageType.request;
	}

	@Override
	public void handleMessage(Connection connection, RequestMessage requestMessage) {
		if (Requests.isHeartbeatRequest(requestMessage)) {
			MessageFactory messageFactory = connection.getProtocol().messageFactory();
			Responses.sendResponse(connection, messageFactory.createResponse(requestMessage.id(),
					requestMessage.serializationType(), ResponseStatus.OK));
			return;
		}
		ResponseMessage responseMessage = handleRequestMessage(requestMessage);
		if (!Requests.isOnewayRequest(requestMessage)) {
			Responses.sendResponse(connection, responseMessage);
		}
	}

	public abstract ResponseMessage handleRequestMessage(RequestMessage requestMessage);

}
