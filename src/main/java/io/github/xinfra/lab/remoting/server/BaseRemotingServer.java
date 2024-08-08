package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionEventHandler;
import io.github.xinfra.lab.remoting.connection.ProtocolDecoder;
import io.github.xinfra.lab.remoting.connection.ProtocolEncoder;
import io.github.xinfra.lab.remoting.connection.ProtocolHandler;
import io.github.xinfra.lab.remoting.connection.ServerConnectionManager;
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
import org.apache.commons.lang3.Validate;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseRemotingServer extends AbstractLifeCycle implements RemotingServer {

    protected SocketAddress localAddress;
    private ServerBootstrap serverBootstrap;

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
    private ChannelHandler handler;
    private ChannelHandler serverIdleHandler = new ServerIdleHandler();

    protected ServerConnectionManager connectionManager;

    private RemotingServerConfig config;

    public BaseRemotingServer(RemotingServerConfig config) {
        Validate.notNull(config, "RemotingServerConfig can not be null");
        Validate.inclusiveBetween(0, 0xFFFF, config.getPort(), "port out of range: " + config.getPort());

        this.config = config;
        this.handler = new ProtocolHandler();

        if (this.config.isManageConnection()) {
            this.connectionManager = new ServerConnectionManager();
            this.connectionEventHandler = new ConnectionEventHandler(this.connectionManager);
        } else {
            this.connectionEventHandler = new ConnectionEventHandler();
        }
    }


    @Override
    public void startup() {
        super.startup();
        if (this.connectionManager != null) {
            this.connectionManager.startup();
        }
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(serverChannelClass)
                .childHandler(
                        new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                ChannelPipeline pipeline = channel.pipeline();

                                pipeline.addLast("encoder", new ProtocolEncoder());
                                pipeline.addLast("decoder", new ProtocolDecoder());

                                if (config.isIdleSwitch()) {
                                    pipeline.addLast("idleStateHandler", new IdleStateHandler(60000, 60000, 0, TimeUnit.MILLISECONDS));
                                    pipeline.addLast("serverIdleHandler", serverIdleHandler);
                                }
                                pipeline.addLast("handler", handler);
                                pipeline.addLast("connectionEventHandler", connectionEventHandler);


                                createConnection(channel);
                            }
                        }
                );


        try {
            this.localAddress = new InetSocketAddress(InetAddress.getLocalHost(), config.getPort());
            ChannelFuture channelFuture = this.serverBootstrap.bind(localAddress).sync();
            if (!channelFuture.isSuccess()) {
                throw channelFuture.cause();
            }
        } catch (Throwable throwable) {
            throw new RuntimeException("serverBootstrap bind fail. ", throwable);
        }
    }

    @AccessForTest
    protected void createConnection(SocketChannel channel) {
        Connection connection = new Connection(protocol(), channel);
        if (config.isManageConnection()) {
            connectionManager.add(connection);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
    }

    @Override
    public SocketAddress localAddress() {
        return this.localAddress;
    }

    @Override
    public void registerUserProcessor(UserProcessor<?> userProcessor) {
        protocol().messageHandler().registerUserProcessor(userProcessor);
    }
}
