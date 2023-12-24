package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class Connection {
    
    public static final AttributeKey<ProtocolType> PROTOCOL = AttributeKey.valueOf("protocol");

    public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");

    private ConcurrentHashMap<Integer, InvokeFuture> invokeMap = new ConcurrentHashMap<>();

    @Getter
    private Channel channel;

    @Getter
    private Endpoint endpoint;


    public Connection(Endpoint endpoint, Channel channel, ProtocolType protocolType) {
        this.endpoint = endpoint;
        this.channel = channel;
        this.channel.attr(PROTOCOL).set(protocolType);
        this.channel.attr(CONNECTION).set(this);
    }


    public void addInvokeFuture(InvokeFuture invokeFuture) {
        invokeFuture.setConnection(this);
        invokeMap.put(invokeFuture.getRequestId(), invokeFuture);
    }

    public InvokeFuture removeInvokeFuture(Integer requestId) {
        return invokeMap.remove(requestId);
    }

    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    public void close() {
        // TODO
    }
}
