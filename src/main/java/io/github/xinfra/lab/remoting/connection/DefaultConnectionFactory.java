package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.exception.RemotingException;
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
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
public class DefaultConnectionFactory implements ConnectionFactory {


    private List<ChannelHandler> channelHandlers;

    private Bootstrap bootstrap;

    private ConnectionConfig connectionConfig;

    // todo EpollUtils
    private static final EventLoopGroup workerGroup = Epoll.isAvailable() ?
            new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors(),
                    new NamedThreadFactory("Remoting-Server-Worker")) :
            new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
                    new NamedThreadFactory("Remoting-Server-Worker"));

    private static final Class<? extends SocketChannel> channelClass = Epoll.isAvailable() ?
            EpollSocketChannel.class : NioSocketChannel.class;

    public DefaultConnectionFactory(List<ChannelHandler> channelHandlers) {
        this(channelHandlers, new ConnectionConfig());
    }

    public DefaultConnectionFactory(List<ChannelHandler> channelHandlers,
                                    ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;

        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(channelClass)
                .handler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        channelHandlers.forEach(pipeline::addLast);

                        if (connectionConfig.isIdleSwitch()) {
                            pipeline.addLast("idleStateHandler", new IdleStateHandler(connectionConfig.getIdleReaderTimeout(),
                                    connectionConfig.getIdleWriterTimeout(), connectionConfig.getIdleAllTimeout(),
                                    TimeUnit.MILLISECONDS));
                        }
                        // todo FlushConsolidationHandler
                    }
                });
    }


    @Override
    public Connection create(Endpoint endpoint) throws RemotingException {
        SocketAddress address = new InetSocketAddress(endpoint.getIp(), endpoint.getPort());
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionConfig.getConnectTimeout());
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
