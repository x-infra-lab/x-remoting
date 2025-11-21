package io.github.xinfra.lab.remoting.impl.handler;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageBody;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.Requests;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.message.Responses;

public class ResponseObserver<R> {

	private final Connection connection;

	private final RequestMessage requestMessage;

	public ResponseObserver(Connection connection, RequestMessage requestMessage) {
		this.connection = connection;
		this.requestMessage = requestMessage;
	}

	public void complete(R result) {
		if (Requests.isOnewayRequest(requestMessage)) {
			return;
		}
		RemotingResponseMessage responseMessage = connection.getProtocol()
			.messageFactory()
			.createResponse(requestMessage.id(), requestMessage.serializationType(), ResponseStatus.OK);
		responseMessage.setBody(new RemotingMessageBody(result));

		Responses.sendResponse(connection, responseMessage);
	}

	public void onError(Throwable t) {
		if (Requests.isOnewayRequest(requestMessage)) {
			return;
		}
		RemotingResponseMessage responseMessage = connection.getProtocol()
			.messageFactory()
			.createResponse(requestMessage.id(), requestMessage.serializationType(), ResponseStatus.Error);
		responseMessage.setBody(new RemotingMessageBody(t));

		Responses.sendResponse(connection, responseMessage);
	}

}
