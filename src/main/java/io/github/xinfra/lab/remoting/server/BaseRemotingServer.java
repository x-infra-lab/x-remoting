package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

@Slf4j
public class BaseRemotingServer extends AbstractLifeCycle implements RemotingServer {

    private SocketAddress localAddress;
    private ServerBootstrap serverBootstrap;

    private final EventLoopGroup bossGroup = Epoll.isAvailable() ?
            new EpollEventLoopGroup(1, new NamedThreadFactory("Remoting-Server-Boss")) :
            new NioEventLoopGroup(1, new NamedThreadFactory("Remoting-Server-Boss"));

    private static final EventLoopGroup workerGroup = Epoll.isAvailable() ?
            new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
                    new NamedThreadFactory("Remoting-Server-Worker")) :
            new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
                    new NamedThreadFactory("Remoting-Server-Worker"));

    private ChannelHandler connectionEventHandler;
    private ChannelHandler encoder;
    private ChannelHandler decoder;
    private ChannelHandler handler;

    public BaseRemotingServer(
            ChannelHandler connectionEventHandler,
            ChannelHandler encoder,
            ChannelHandler decoder,
            ChannelHandler handler) {
        this.connectionEventHandler = connectionEventHandler;
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
    }

    public BaseRemotingServer(SocketAddress localAddress,
                              ChannelHandler connectionEventHandler,
                              ChannelHandler encoder,
                              ChannelHandler decoder,
                              ChannelHandler handler) {
        this.localAddress = localAddress;

        this.connectionEventHandler = connectionEventHandler;
        this.encoder = encoder;
        this.decoder = decoder;
        this.handler = handler;
    }


    @Override
    public void startup() {
        super.startup();
        this.serverBootstrap = new ServerBootstrap();

        this.serverBootstrap
                .group(bossGroup, workerGroup)
                .childHandler(
                        new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                ChannelPipeline pipeline = channel.pipeline();

                                pipeline.addLast("connectionEventHandler", connectionEventHandler);
                                pipeline.addLast("encoder", encoder);
                                pipeline.addLast("decoder", decoder);
                                pipeline.addLast("handler", handler);
                            }
                        }
                );


        try {
            ChannelFuture channelFuture;
            if (localAddress == null) {
                channelFuture = this.serverBootstrap.bind().sync();
            } else {
                channelFuture = this.serverBootstrap.bind(localAddress).sync();
            }

            if (!channelFuture.isSuccess()) {
                throw channelFuture.cause();
            } else if (localAddress == null) {
                this.localAddress = channelFuture.channel().localAddress();
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
    public SocketAddress localAddress() {
        return this.localAddress;
    }

    @Override
    public void registerUserProcessor(UserProcessor<?> userProcessor) {
        // TODO
    }
}
