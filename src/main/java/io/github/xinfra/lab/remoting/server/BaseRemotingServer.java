package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.connection.ConnectionEventHandler;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.connection.DefaultConnectionManager;
import io.github.xinfra.lab.remoting.connection.ProtocolDecoder;
import io.github.xinfra.lab.remoting.connection.ProtocolEncoder;
import io.github.xinfra.lab.remoting.connection.ProtocolHandler;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseRemotingServer extends AbstractLifeCycle implements RemotingServer {

    private InetSocketAddress localAddress;
    private ServerBootstrap serverBootstrap;

    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors = new ConcurrentHashMap<>();

    private final EventLoopGroup bossGroup = Epoll.isAvailable() ?
            new EpollEventLoopGroup(1, new NamedThreadFactory("Remoting-Server-Boss")) :
            new NioEventLoopGroup(1, new NamedThreadFactory("Remoting-Server-Boss"));

    private static final EventLoopGroup workerGroup = Epoll.isAvailable() ?
            new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
                    new NamedThreadFactory("Remoting-Server-Worker")) :
            new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
                    new NamedThreadFactory("Remoting-Server-Worker"));

    private static final Class<? extends ServerChannel> serverChannelClass = Epoll.isAvailable() ?
            EpollServerSocketChannel.class : NioServerSocketChannel.class;

    private ChannelHandler connectionEventHandler;
    private ChannelHandler encoder;
    private ChannelHandler decoder;
    private ChannelHandler handler;
    private ChannelHandler serverIdleHandler = new ServerIdleHandler();

    private boolean manageConnection;
    private ConnectionManager connectionManager;

    public BaseRemotingServer(InetSocketAddress localAddress,
                              boolean manageConnection) {
        this.connectionEventHandler = new ConnectionEventHandler();
        this.encoder = new ProtocolEncoder();
        this.decoder = new ProtocolDecoder();
        this.handler = new ProtocolHandler(userProcessors);

        this.localAddress = localAddress;

        this.manageConnection = manageConnection;
        if (this.manageConnection){
            // TODO
//            connectionManager = new DefaultConnectionManager();
        }
    }

    public BaseRemotingServer(int port) {
        this(new InetSocketAddress(port), false);
    }

    /**
     *
     * @param port
     * @param manageConnection
     */
    public BaseRemotingServer(int port, boolean manageConnection) {
        this(new InetSocketAddress(port), manageConnection);
    }


    @Override
    public void startup() {
        super.startup();
        this.serverBootstrap = new ServerBootstrap();

        this.serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(serverChannelClass)
                .childHandler(
                        new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                ChannelPipeline pipeline = channel.pipeline();

                                pipeline.addLast("connectionEventHandler", connectionEventHandler);
                                pipeline.addLast("encoder", encoder);
                                pipeline.addLast("decoder", decoder);

                                // todo: use config
                                pipeline.addLast("idleStateHandler", new IdleStateHandler(1500, 1500, 0, TimeUnit.MILLISECONDS));
                                pipeline.addLast("serverIdleHandler", serverIdleHandler);
                                pipeline.addLast("handler", handler);
                            }
                        }
                );


        try {
            ChannelFuture channelFuture = this.serverBootstrap.bind(localAddress).sync();

            if (!channelFuture.isSuccess()) {
                throw channelFuture.cause();
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("serverBootstrap bind fail. ", throwable);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public InetSocketAddress localAddress() {
        return this.localAddress;
    }

    @Override
    public void registerUserProcessor(UserProcessor<?> userProcessor) {
        UserProcessor<?> oldUserProcessor = userProcessors.put(userProcessor.interest(), userProcessor);
        if (oldUserProcessor != null) {
            log.warn("registered userProcessor change from:{} to:{}", oldUserProcessor, userProcessor);
        }
    }
}
