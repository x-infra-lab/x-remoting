package io.github.xinfra.lab.remoting.connection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.net.SocketAddress;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;


@ChannelHandler.Sharable
@Slf4j
public class ConnectionEventHandler extends ChannelDuplexHandler {

    private ConnectionManager connectionManager;

    public ConnectionEventHandler(ConnectionManager connectionManager) {
        Validate.notNull(connectionManager, "connectionManager can not be null.");
        this.connectionManager = connectionManager;
    }

    public ConnectionEventHandler() {
        // server side do not manage connections
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        userEventTriggered(ctx, ConnectionEvent.CONNECT);

        super.channelActive(ctx);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        Connection connection = ctx.channel().attr(CONNECTION).get();
        if (connection != null) {
            connection.onClose();
        }

        super.close(ctx, promise);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Connection connection = ctx.channel().attr(CONNECTION).get();
        if (connectionManager != null) {
            connectionManager.removeAndClose(connection);
        }
        userEventTriggered(ctx, ConnectionEvent.CLOSE);

        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ConnectionEvent) {
            Connection connection = ctx.channel().attr(CONNECTION).get();
            ConnectionEvent connectionEvent = (ConnectionEvent) evt;
            if (connectionEvent == ConnectionEvent.CLOSE) {
                connectionManager.reconnect(connection.getEndpoint());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        SocketAddress localAddress = channel.localAddress();
        SocketAddress remoteAddress = channel.remoteAddress();

        log.warn("exceptionCaught channel localAddress:{} remoteAddress:{}, close the channel! cause by",
                localAddress, remoteAddress, cause);

        channel.close();
    }
}
