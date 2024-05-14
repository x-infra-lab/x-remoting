package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Slf4j
public class DefaultConnectionFactory implements ConnectionFactory {

    private ChannelHandler connectionEventHandler;
    private ChannelHandler encoder;
    private ChannelHandler decoder;
    private ChannelHandler heartbeatHandler;
    private ChannelHandler handler;
    private Bootstrap bootstrap;

    private static final EventLoopGroup workerGroup = Epoll.isAvailable() ?
            new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors(),
                    new NamedThreadFactory("Remoting-Server-Worker")) :
            new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
                    new NamedThreadFactory("Remoting-Server-Worker"));

    private static final Class<? extends SocketChannel> channelClass= Epoll.isAvailable()?
            EpollSocketChannel.class : NioSocketChannel.class;

    private ConnectionManager connectionManager;

    public DefaultConnectionFactory(ConnectionManager connectionManager,
                                    ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        this(new ConnectionEventHandler(connectionManager),
                new ProtocolEncoder(),
                new ProtocolDecoder(),
                new ProtocolHeartBeatHandler(),
                new ProtocolHandler(userProcessors)
        );
        this.connectionManager = connectionManager;
    }

    public DefaultConnectionFactory(
            ChannelHandler connectionEventHandler,
            ChannelHandler encoder,
            ChannelHandler decoder,
            ChannelHandler heartbeatHandler,
            ChannelHandler handler) {
        this.connectionEventHandler = connectionEventHandler;
        this.encoder = encoder;
        this.decoder = decoder;
        this.heartbeatHandler = heartbeatHandler;
        this.handler = handler;

        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(channelClass)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("connectionEventHandler", connectionEventHandler);
                        pipeline.addLast("encoder", encoder);
                        pipeline.addLast("decoder", decoder);

                        // todo: use config
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(1500, 1500, 0, TimeUnit.MILLISECONDS));
                        pipeline.addLast("heartbeatHandler", heartbeatHandler);
                        pipeline.addLast("handler", handler);
                    }
                });
    }


    @Override
    public Connection create(Endpoint endpoint, ConnectionConfig config) throws RemotingException {
        SocketAddress address = new InetSocketAddress(endpoint.getIp(), endpoint.getPort());
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeoutMills());
        ChannelFuture future = bootstrap.connect(address);
        future.awaitUninterruptibly();
        if (!future.isDone()) {
            String errMsg = "Create connection to " + address + " timeout!";
            log.warn(errMsg);
            throw new RemotingException(errMsg);
        }
        if (future.isCancelled()) {
            String errMsg = "Create connection to " + address + " cancelled by user!";
            log.warn(errMsg);
            throw new RemotingException(errMsg);
        }
        if (!future.isSuccess()) {
            String errMsg = "Create connection to " + address + " error!";
            log.warn(errMsg);
            throw new RemotingException(errMsg, future.cause());
        }
        Channel channel = future.channel();
        return new Connection(endpoint, channel);
    }

}
