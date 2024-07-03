package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.channel.ChannelHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class ClientConnectionManager extends AbstractConnectionManager {

    public ClientConnectionManager(ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {

        this.connectionFactory = new DefaultConnectionFactory(defaultChannelHandlers(userProcessors));
    }

    public ClientConnectionManager(ConcurrentHashMap<String, UserProcessor<?>> userProcessors,
                                   ConnectionConfig connectionConfig) {
        this.connectionFactory = new DefaultConnectionFactory(defaultChannelHandlers(userProcessors), connectionConfig);
    }

    private List<ChannelHandler> defaultChannelHandlers(ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        List<ChannelHandler> channelHandlers = new ArrayList<>();
        channelHandlers.add(new ProtocolEncoder());
        channelHandlers.add(new ProtocolDecoder());
        channelHandlers.add(new ProtocolHeartBeatHandler());
        channelHandlers.add(new ProtocolHandler(userProcessors));
        channelHandlers.add(new ConnectionEventHandler(this));
        return channelHandlers;
    }
}
