package io.github.xinfra.lab.remoting.rpc.handler;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.message.RemotingMessageBody;
import io.github.xinfra.lab.remoting.rpc.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.RequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.Requests;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.message.Responses;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;

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
		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		RemotingResponseMessage responseMessage = protocol.getMessageFactory()
			.createResponse(requestMessage.getId(), requestMessage.getSerializationType(), ResponseStatus.OK);
		responseMessage.setBody(new RemotingMessageBody(result));

		Responses.sendResponse(connection, responseMessage);
	}

	public void onError(Throwable t) {
		if (Requests.isOnewayRequest(requestMessage)) {
			return;
		}
		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		RemotingResponseMessage responseMessage = protocol.getMessageFactory()
			.createResponse(requestMessage.getId(), requestMessage.getSerializationType(), ResponseStatus.Error);
		responseMessage.setBody(new RemotingMessageBody(t));

		Responses.sendResponse(connection, responseMessage);
	}

}
