package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;

public class HeartbeatRequestMessageTypeHandler implements MessageTypeHandler<RequestMessage>{
    @Override
    public MessageType messageType() {
        return MessageType.heartbeatRequest;
    }

    @Override
    public void handleMessage(Connection connection, RequestMessage requestMessage) {
        ResponseMessage responseMessage =  connection.getProtocol().messageFactory().createResponse(requestMessage.id(),
                requestMessage.serializationType(), ResponseStatus.OK);
        Responses.sendResponse(connection, responseMessage);
    }
}
