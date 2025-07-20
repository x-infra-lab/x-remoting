package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@Slf4j
public abstract class AbstractMessageHandler implements MessageHandler {

    public AbstractMessageHandler() {
        registerMessageTypeHandler();
    }

    @Override
    public void handleMessage(ChannelHandlerContext ctx, Message msg) {
        Connection connection = ctx.channel().attr(CONNECTION).get();
        try {
            // todo handle heartbeat
            messageTypeHandler(msg.messageType()).handleMessage(connection, msg);
        }
        catch (Exception e) {
            // todo log
            ResponseMessage response = connection.getProtocol()
                    .messageFactory()
                    .createResponse(msg.id(), ResponseStatus.Error, e);
            Responses.sendResponse(connection, response);
        }
    }
}
