package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;

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

    private Set<Endpoint> disableReconnectEndpoints = new CopyOnWriteArraySet<>();

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
        for (Endpoint endpoint : connections.keySet()) {
            disableReconnect(endpoint);
        }
        super.shutdown();

        reconnector.shutdownNow();
    }

    @Override
    public synchronized void reconnect(Endpoint endpoint) throws RemotingException {
        ensureStarted();
        if (disableReconnectEndpoints.contains(endpoint)) {
            log.warn("endpoint:{} is disable to reconnect", endpoint);
            throw new RemotingException("endpoint is disable to reconnect:" + endpoint);
        }
        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            connectionHolder = createConnectionHolder(endpoint);
            createConnectionForHolder(endpoint, connectionHolder, config.getConnectionNumPreEndpoint());
        } else {
            int needCreateNum = config.getConnectionNumPreEndpoint() - connectionHolder.size();
            if (needCreateNum > 0) {
                createConnectionForHolder(endpoint, connectionHolder, needCreateNum);
            }
        }
    }

    @Override
    public synchronized void disableReconnect(Endpoint endpoint) {
        ensureStarted();
        disableReconnectEndpoints.add(endpoint);
    }

    @Override
    public synchronized void enableReconnect(Endpoint endpoint) {
        ensureStarted();
        disableReconnectEndpoints.remove(endpoint);
    }

    @Override
    public synchronized Future<Void> asyncReconnect(Endpoint endpoint) {
        ensureStarted();
        if (disableReconnectEndpoints.contains(endpoint)) {
            log.warn("endpoint:{} is disable to asyncReconnect", endpoint);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new RemotingException("endpoint is disable to asyncReconnect:" + endpoint));
            return future;
        }

        Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    reconnect(endpoint);
                } catch (Exception e) {
                    log.warn("reconnect endpoint:{} fail", endpoint, e);
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
