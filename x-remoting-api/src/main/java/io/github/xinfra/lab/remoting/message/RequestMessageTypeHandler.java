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
        MessageExchange messageExchange = new MessageExchange(requestMessage, connection);
        if (Requests.isHeartbeatRequest(requestMessage)) {
            ResponseMessage responseMessage =  connection.getProtocol().messageFactory().createResponse(requestMessage.id(),
                    requestMessage.serializationType(), ResponseStatus.OK);
            messageExchange.sendResponse(responseMessage);
			return;
		}
		 handleRequestMessage(messageExchange);
	}

    public abstract void handleRequestMessage(MessageExchange messageExchange);


}
