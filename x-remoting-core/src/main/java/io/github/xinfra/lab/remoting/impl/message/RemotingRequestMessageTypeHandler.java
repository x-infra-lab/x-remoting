package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.impl.handler.HandlerRegistry;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.RequestMessageTypeHandler;
import io.github.xinfra.lab.remoting.message.ResponseMessage;

public class RemotingRequestMessageTypeHandler extends RequestMessageTypeHandler {

    private HandlerRegistry handlerRegistry;

    public RemotingRequestMessageTypeHandler(HandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public ResponseMessage handleRequestMessage(RequestMessage requestMessage) {
        return null;
    }
}
