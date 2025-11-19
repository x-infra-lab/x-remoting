package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandler;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.impl.handler.ResponseObserver;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.RequestMessageTypeHandler;
import io.github.xinfra.lab.remoting.message.MessageExchange;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemotingRequestMessageTypeHandler extends RequestMessageTypeHandler {

    private RequestHandlerRegistry requestHandlerRegistry;

    public RemotingRequestMessageTypeHandler(RequestHandlerRegistry requestHandlerRegistry) {
        this.requestHandlerRegistry = requestHandlerRegistry;
    }

    @Override
    public void handleRequestMessage(MessageExchange messageExchange) {

        RequestMessage requestMessage = messageExchange.getRequestMessage();
        Connection connection = messageExchange.getConnection();
        RequestHandler requestHandler = requestHandlerRegistry.lookup(requestMessage.path());
        if (requestHandler == null) {
            log.warn("RequestHandler not found for path: {}", requestMessage.path());
            messageExchange.sendResponse(connection.getProtocol()
                    .messageFactory()
                    .createResponse(requestMessage.id(), requestMessage.serializationType(),
                            ResponseStatus.NotFound));
            return;
        }
        requestHandler.asyncHandle(requestMessage,
                new ResponseObserver(messageExchange));
    }

}
