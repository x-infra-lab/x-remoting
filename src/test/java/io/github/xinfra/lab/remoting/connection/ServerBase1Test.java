package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LoggingHandler;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;
import static io.github.xinfra.lab.remoting.rpc.RpcProtocol.RPC;

public class ServerBase1Test {

    public static int serverPort;

    public static String remoteAddress = "localhost";

    public static Channel serverChannel;

    @BeforeClass
    public static void beforeClass() throws InterruptedException {
        // start a http server
        serverPort = findAvailableTcpPort();
        ChannelFuture future = new ServerBootstrap()
                .group(new NioEventLoopGroup(1))
                .channel(NioServerSocketChannel.class)
                .handler(new HttpServerCodec())
                .childHandler(new LoggingHandler())
                .bind(serverPort);
        Assert.assertTrue(future.sync().isSuccess());
        serverChannel = future.channel();
    }

    @AfterClass
    public static void afterClass() throws InterruptedException {
        if (serverChannel != null) {
            Assert.assertTrue(serverChannel.close().sync().isSuccess());
        }
    }
}
