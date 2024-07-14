package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.channel.ChannelHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


public class ClientConnectionManager extends AbstractConnectionManager {

    public ClientConnectionManager(ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {

        this.connectionFactory = new DefaultConnectionFactory(defaultChannelSuppliers(userProcessors));
    }

    public ClientConnectionManager(ConcurrentHashMap<String, UserProcessor<?>> userProcessors,
                                   ConnectionConfig connectionConfig) {
        this.connectionFactory = new DefaultConnectionFactory(defaultChannelSuppliers(userProcessors), connectionConfig);
    }

    public ClientConnectionManager(ConcurrentHashMap<String, UserProcessor<?>> userProcessors,
                                   ConnectionManagerConfig connectionManagerConfig) {
        super(connectionManagerConfig);
        this.connectionFactory = new DefaultConnectionFactory(defaultChannelSuppliers(userProcessors));
    }

    public ClientConnectionManager(ConcurrentHashMap<String, UserProcessor<?>> userProcessors,
                                   ConnectionConfig connectionConfig,
                                   ConnectionManagerConfig connectionManagerConfig) {
        super(connectionManagerConfig);
        this.connectionFactory = new DefaultConnectionFactory(defaultChannelSuppliers(userProcessors), connectionConfig);
    }

    private List<Supplier<ChannelHandler>> defaultChannelSuppliers(ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        ProtocolHeartBeatHandler protocolHeartBeatHandler = new ProtocolHeartBeatHandler();
        ProtocolHandler protocolHandler = new ProtocolHandler(userProcessors);
        ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler(this);

        List<Supplier<ChannelHandler>> channelHandlerSuppliers = new ArrayList<>();
        channelHandlerSuppliers.add(() -> new ProtocolEncoder());
        channelHandlerSuppliers.add(() -> new ProtocolDecoder());
        channelHandlerSuppliers.add(() -> protocolHeartBeatHandler);
        channelHandlerSuppliers.add(() -> protocolHandler);
        channelHandlerSuppliers.add(() -> connectionEventHandler);
        return channelHandlerSuppliers;
    }

}
