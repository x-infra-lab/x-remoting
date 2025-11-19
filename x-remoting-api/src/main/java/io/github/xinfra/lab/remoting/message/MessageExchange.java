package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.common.Requests;
import io.github.xinfra.lab.remoting.connection.Connection;
import lombok.Getter;

public class MessageExchange {

    @Getter
    private final RequestMessage requestMessage;
    @Getter
    private final Connection connection;


    public MessageExchange(RequestMessage requestMessage, Connection connection) {
        this.requestMessage = requestMessage;
        this.connection = connection;
    }

    public void sendResponse(ResponseMessage responseMessage) {
        if (Requests.isOnewayRequest(requestMessage)){
            return;
        }
        Responses.sendResponse(connection, responseMessage);
    }
}
