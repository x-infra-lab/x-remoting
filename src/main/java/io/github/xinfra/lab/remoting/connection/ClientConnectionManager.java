package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.channel.ChannelHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        // encoder and decoder not @ChannelHandler.Sharable marked. it need create instance everytime
        channelHandlerSuppliers.add(ProtocolEncoder::new);
        channelHandlerSuppliers.add(ProtocolDecoder::new);
        channelHandlerSuppliers.add(() -> protocolHeartBeatHandler);
        channelHandlerSuppliers.add(() -> protocolHandler);
        channelHandlerSuppliers.add(() -> connectionEventHandler);
        return channelHandlerSuppliers;
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();

        for (Map.Entry<Endpoint, ConnectionHolder> entry : connections.entrySet()) {
            Endpoint endpoint = entry.getKey();
            disableReconnect(endpoint);
            ConnectionHolder connectionHolder = entry.getValue();
            connectionHolder.removeAndCloseAll();
            connections.remove(endpoint);
        }

    }
}
