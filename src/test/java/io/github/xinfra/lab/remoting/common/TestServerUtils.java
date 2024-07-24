package io.github.xinfra.lab.remoting.common;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.junit.jupiter.api.Assertions;

public class TestServerUtils {
    static final int retryNum = 3;

    public static NioServerSocketChannel startEmptyServer() throws InterruptedException {

        for (int i = 0; i < retryNum; i++) {
            try {
                int serverPort = TestSocketUtils.findAvailableTcpPort();
                ChannelFuture future = new ServerBootstrap()
                        .group(new NioEventLoopGroup(1))
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new LoggingHandler())
                        .bind(serverPort);
                Assertions.assertTrue(future.sync().isSuccess());
                Assertions.assertTrue(future.channel().isActive());
                return (NioServerSocketChannel) future.channel();
            } catch (Throwable throwable) {
                // ignore
            }
        }
        throw new RuntimeException("startEmptyServer fail.");
    }

}
