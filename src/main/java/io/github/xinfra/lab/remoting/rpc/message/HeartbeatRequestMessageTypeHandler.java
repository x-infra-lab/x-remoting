package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;

public class HeartbeatRequestMessageTypeHandler implements MessageTypeHandler<RequestMessage> {

	@Override
	public MessageType getMessageType() {
		return MessageType.heartbeatRequest;
	}

	@Override
	public void handleMessage(Connection connection, RequestMessage requestMessage) {
		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		ResponseMessage responseMessage = protocol.getMessageFactory()
			.createResponse(requestMessage.getId(), requestMessage.getSerializationType(), ResponseStatus.OK);
		Responses.sendResponse(connection, responseMessage);
	}

}
