package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.TestServerUtils;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class ConnectionFactoryTest {

    private ProtocolType test = new ProtocolType("ConnectionFactoryTest", "ConnectionFactoryTest".getBytes());

    private static String remoteAddress;
    private static int serverPort;

    private static NioServerSocketChannel serverSocketChannel;

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        serverSocketChannel = TestServerUtils.startEmptyServer();
        remoteAddress = serverSocketChannel.localAddress().getHostName();
        serverPort = serverSocketChannel.localAddress().getPort();
    }

    @AfterAll
    public static void afterAll() throws InterruptedException {
        serverSocketChannel.close().sync();
    }

    @Test
    public void testNewInstance() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new DefaultConnectionFactory(null, null);
        });

        Assertions.assertThrows(NullPointerException.class, () -> {
            new DefaultConnectionFactory(null);
        });

        // empty channelHandlers is fine
        Assertions.assertNotNull(new DefaultConnectionFactory(new ArrayList<>()));
    }

    @Test
    public void testCreateSuccess() throws RemotingException {
        List<Supplier<ChannelHandler>> channelHandlerSuppliers = new ArrayList<>();
        channelHandlerSuppliers.add(() -> new HttpClientCodec());
        ConnectionFactory connectionFactory = new DefaultConnectionFactory(channelHandlerSuppliers);

        Connection connection = connectionFactory.create(new Endpoint(test, remoteAddress, serverPort));
        Assertions.assertNotNull(connection);

        Channel channel = connection.getChannel();
        Assertions.assertNotNull(channel);
        Assertions.assertTrue(channel.isActive());

        List<Class<?>> handlerClassListForPipeline = channel.pipeline().
                toMap().values().
                stream().map(v -> v.getClass()).
                collect(Collectors.toList());

        List<Class<?>> handerClassList = channelHandlerSuppliers.stream().map(v -> v.get()).map(v -> v.getClass())
                .collect(Collectors.toList());

        Assertions.assertTrue(handlerClassListForPipeline.containsAll(handerClassList));
        Assertions.assertEquals(((InetSocketAddress) channel.remoteAddress()).getHostName(), remoteAddress);
        connection.close();
    }

    @Test
    public void testCreateFail() {
        List<Supplier<ChannelHandler>> channelHandlerSuppliers = new ArrayList<>();
        channelHandlerSuppliers.add(() -> new HttpClientCodec());
        ConnectionFactory connectionFactory = new DefaultConnectionFactory(channelHandlerSuppliers);

        Endpoint invalidEndpoint = new Endpoint(test, remoteAddress, serverPort + 1);
        Assertions.assertThrows(RemotingException.class, () -> {
            connectionFactory.create(invalidEndpoint);
        });

    }

}
