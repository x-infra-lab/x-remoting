package io.github.xinfra.lab.remoting.connection;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LoggingHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;

public class ServerBase1Test {

    public static int serverPort;

    public static String remoteAddress = "localhost";

    public static Channel serverChannel;

    @BeforeAll
    public static void beforeClass() throws InterruptedException {
        // start a http server
        if (serverChannel != null)
            return;
        System.out.println("ServerBase1Test start a http server serverPort:" + serverPort);
        serverPort = findAvailableTcpPort();
        ChannelFuture future = new ServerBootstrap()
                .group(new NioEventLoopGroup(1))
                .channel(NioServerSocketChannel.class)
                .handler(new HttpServerCodec())
                .childHandler(new LoggingHandler())
                .bind(serverPort);
        Assertions.assertTrue(future.sync().isSuccess());
        serverChannel = future.channel();
    }

}
