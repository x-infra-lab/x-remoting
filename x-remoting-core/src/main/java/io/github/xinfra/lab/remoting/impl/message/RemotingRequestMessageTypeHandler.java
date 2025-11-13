package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.impl.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.RequestMessageTypeHandler;
import io.github.xinfra.lab.remoting.message.ResponseMessage;

public class RemotingRequestMessageTypeHandler extends RequestMessageTypeHandler {

    private RequestHandlerRegistry requestHandlerRegistry;

    public RemotingRequestMessageTypeHandler(RequestHandlerRegistry requestHandlerRegistry) {
        this.requestHandlerRegistry = requestHandlerRegistry;
    }

    @Override
    public ResponseMessage handleRequestMessage(RequestMessage requestMessage) {
        return null;
    }
}
