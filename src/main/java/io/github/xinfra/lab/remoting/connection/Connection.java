package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Connection {

    public static final AttributeKey<Protocol> PROTOCOL = AttributeKey.valueOf("protocol");

    public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");


    public static final AttributeKey<Integer> HEARTBEAT_FAIL_COUNT = AttributeKey.valueOf("heartbeat_fail_count");

    @AccessForTest
    protected ConcurrentHashMap<Integer, InvokeFuture> invokeMap = new ConcurrentHashMap<>();

    @Getter
    private Channel channel;

    @Getter
    private Protocol protocol;

    private final AtomicBoolean closed = new AtomicBoolean(false);


    public Connection(Protocol protocol, Channel channel) {
        Validate.notNull(protocol, "protocol can not be null");
        Validate.notNull(channel, "channel can not be null");
        this.protocol = protocol;
        this.channel = channel;
        this.channel.attr(PROTOCOL).set(protocol);
        this.channel.attr(CONNECTION).set(this);
        this.channel.attr(HEARTBEAT_FAIL_COUNT).set(0);
    }


    public void addInvokeFuture(InvokeFuture invokeFuture) {
        InvokeFuture prevFuture = invokeMap.put(invokeFuture.getRequestId(), invokeFuture);
        Validate.isTrue(prevFuture == null, "requestId: %s already invoked", invokeFuture.getRequestId());
    }

    public InvokeFuture removeInvokeFuture(Integer requestId) {
        return invokeMap.remove(requestId);
    }

    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    public ChannelFuture close() {
        if (closed.compareAndSet(false, true)) {
            return channel.close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.info("close connection to remote address:{} success:{} fail cause:{}",
                            channel.remoteAddress(), future.isSuccess(), future.cause());
                }
            });
        }
        return channel.newSucceededFuture();
    }

    public void onClose() {
        for (int requestId : invokeMap.keySet()) {
            InvokeFuture invokeFuture = removeInvokeFuture(requestId);
            if (invokeFuture != null) {
                invokeFuture.cancelTimeout();
                invokeFuture.complete(createConnectionClosedMessage(requestId));
                invokeFuture.asyncExecuteCallBack();
            }
        }
    }

    private Message createConnectionClosedMessage(int requestId) {
        return protocol.messageFactory().createConnectionClosedMessage(requestId, remoteAddress());
    }
}
