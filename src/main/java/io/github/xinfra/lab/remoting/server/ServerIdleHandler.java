package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@ChannelHandler.Sharable
@Slf4j
public class ServerIdleHandler extends ChannelDuplexHandler {


    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            try {
                log.warn("Connection idle, close it from server side: {}",
                        ctx.channel().remoteAddress());
                Connection connection = ctx.channel().attr(CONNECTION).get();
                connection.close();
            } catch (Exception e) {
                log.warn("Exception caught when closing connection in ServerIdleHandler.", e);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
