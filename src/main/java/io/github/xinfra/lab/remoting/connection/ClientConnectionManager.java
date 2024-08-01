package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


@Slf4j
public class ClientConnectionManager extends AbstractConnectionManager {
    private ExecutorService reconnector = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1024),
            new NamedThreadFactory("Reconnector-Worker"));

    private Set<SocketAddress> disableReconnectSocketAddresses = new CopyOnWriteArraySet<>();

    public ClientConnectionManager(Protocol protocol,
                                   ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        this.connectionFactory = new DefaultConnectionFactory(protocol,
                defaultChannelSuppliers(userProcessors));
    }

    public ClientConnectionManager(Protocol protocol,
                                   ConcurrentHashMap<String, UserProcessor<?>> userProcessors,
                                   ConnectionConfig connectionConfig) {
        this.connectionFactory = new DefaultConnectionFactory(protocol,
                defaultChannelSuppliers(userProcessors), connectionConfig);
    }

    public ClientConnectionManager(Protocol protocol,
                                   ConcurrentHashMap<String, UserProcessor<?>> userProcessors,
                                   ConnectionManagerConfig connectionManagerConfig) {
        super( connectionManagerConfig);
        this.connectionFactory = new DefaultConnectionFactory(protocol,
                defaultChannelSuppliers(userProcessors));
    }

    public ClientConnectionManager(Protocol protocol,
                                   ConcurrentHashMap<String, UserProcessor<?>> userProcessors,
                                   ConnectionConfig connectionConfig,
                                   ConnectionManagerConfig connectionManagerConfig) {
        super( connectionManagerConfig);
        this.connectionFactory = new DefaultConnectionFactory(protocol,
                defaultChannelSuppliers(userProcessors), connectionConfig);
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
        for (SocketAddress socketAddress : connections.keySet()) {
            disableReconnect(socketAddress);
        }
        super.shutdown();

        reconnector.shutdownNow();
    }

    @Override
    public synchronized void reconnect(SocketAddress socketAddress) throws RemotingException {
        ensureStarted();
        if (disableReconnectSocketAddresses.contains(socketAddress)) {
            log.warn("socketAddress:{} is disable to reconnect", socketAddress);
            throw new RemotingException("socketAddress is disable to reconnect:" + socketAddress);
        }
        ConnectionHolder connectionHolder = connections.get(socketAddress);
        if (connectionHolder == null) {
            connectionHolder = createConnectionHolder(socketAddress);
            createConnectionForHolder(socketAddress, connectionHolder, config.getConnectionNumPreEndpoint());
        } else {
            int needCreateNum = config.getConnectionNumPreEndpoint() - connectionHolder.size();
            if (needCreateNum > 0) {
                createConnectionForHolder(socketAddress, connectionHolder, needCreateNum);
            }
        }
    }

    @Override
    public synchronized void disableReconnect(SocketAddress socketAddress) {
        ensureStarted();
        disableReconnectSocketAddresses.add(socketAddress);
    }

    @Override
    public synchronized void enableReconnect(SocketAddress socketAddress) {
        ensureStarted();
        disableReconnectSocketAddresses.remove(socketAddress);
    }

    @Override
    public synchronized Future<Void> asyncReconnect(SocketAddress socketAddress) {
        ensureStarted();
        if (disableReconnectSocketAddresses.contains(socketAddress)) {
            log.warn("socketAddress:{} is disable to asyncReconnect", socketAddress);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new RemotingException("socketAddress is disable to asyncReconnect:" + socketAddress));
            return future;
        }

        Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    reconnect(socketAddress);
                } catch (Exception e) {
                    log.warn("reconnect socketAddress:{} fail", socketAddress, e);
                    throw e;
                }
                return null;
            }
        };

        try {
            return reconnector.submit(callable);
        } catch (Throwable t) {
            log.warn("asyncReconnect submit failed.", t);
            throw t;
        }
    }
}
