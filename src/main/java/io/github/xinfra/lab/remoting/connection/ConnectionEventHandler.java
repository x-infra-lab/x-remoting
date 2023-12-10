package io.github.xinfra.lab.remoting.connection;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
public class ConnectionEventHandler extends ChannelDuplexHandler {

    private ConnectionManager connectionManager;

    public ConnectionEventHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // TODO
}
