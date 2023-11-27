package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;


@Slf4j
public abstract class AbstractConnectionFactory implements ConnectionFactory {
    private ProtocolType protocolType;
    private ChannelHandler encoder;
    private ChannelHandler decoder;
    private ChannelHandler heartbeatHandler;
    private ChannelHandler handler;

    private Bootstrap bootstrap;


    public AbstractConnectionFactory(ProtocolType protocolType,
                                     ChannelHandler encoder,
                                     ChannelHandler decoder,
                                     ChannelHandler heartbeatHandler,
                                     ChannelHandler handler) {
        this.protocolType = protocolType;
        this.encoder = encoder;
        this.decoder = decoder;
        this.heartbeatHandler = heartbeatHandler;
        this.handler = handler;

        bootstrap = new Bootstrap();
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("encoder", encoder);
                pipeline.addLast("decoder", decoder);

                pipeline.addLast("idleStateHandler", new IdleStateHandler(1500, 1500, 0, TimeUnit.MILLISECONDS));
                pipeline.addLast("heartbeatHandler", heartbeatHandler);
                pipeline.addLast("handler", handler);
            }
        });
    }


    @Override
    public Connection create(Endpoint endpoint) throws RemotingException {
        SocketAddress address = new InetSocketAddress(endpoint.getIp(), endpoint.getPort());
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, endpoint.getConnectTimeoutMills());
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
        return new Connection(endpoint, channel, protocolType);
    }
}
