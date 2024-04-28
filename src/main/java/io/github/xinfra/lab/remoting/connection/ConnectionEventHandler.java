package io.github.xinfra.lab.remoting.connection;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;


@ChannelHandler.Sharable
public class ConnectionEventHandler extends ChannelDuplexHandler {

    private ConnectionManager connectionManager;

    public ConnectionEventHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public ConnectionEventHandler() {
    }


    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        // TODO connection#onclose
        super.close(ctx, promise);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // TODO
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // TODO
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // TODO
        super.exceptionCaught(ctx, cause);
    }
}
