package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.xinfra.lab.remoting.rpc.RpcProtocol.RPC;


public class ConnectionFactoryTest extends ServerBase1Test {


    @Test
    public void testNewInstance() {
        Assert.assertThrows(NullPointerException.class, () -> {
            new DefaultConnectionFactory(null, null);
        });

        Assert.assertThrows(NullPointerException.class, () -> {
            new DefaultConnectionFactory(null);
        });

        // empty channelHandlers is fine
        Assert.assertNotNull(new DefaultConnectionFactory(new ArrayList<>()));
    }

    @Test
    public void testCreateSuccess() throws RemotingException {
        List<Supplier<ChannelHandler>> channelHandlerSuppliers = new ArrayList<>();
        channelHandlerSuppliers.add(() -> new HttpClientCodec());
        ConnectionFactory connectionFactory = new DefaultConnectionFactory(channelHandlerSuppliers);

        Connection connection = connectionFactory.create(new Endpoint(RPC, remoteAddress, serverPort));
        Assert.assertNotNull(connection);

        Channel channel = connection.getChannel();
        Assert.assertNotNull(channel);
        Assert.assertTrue(channel.isActive());

        List<Class<?>> handlerClassListForPipeline = channel.pipeline().
                toMap().values().
                stream().map(v -> v.getClass()).
                collect(Collectors.toList());

        List<Class<?>> handerClassList = channelHandlerSuppliers.stream().map(v -> v.get()).map(v -> v.getClass())
                .collect(Collectors.toList());

        Assert.assertTrue(handlerClassListForPipeline.containsAll(handerClassList));
        Assert.assertEquals(((InetSocketAddress) channel.remoteAddress()).getHostName(), remoteAddress);
        connection.close();
    }

    @Test
    public void testCreateFail() {
        List<Supplier<ChannelHandler>> channelHandlerSuppliers = new ArrayList<>();
        channelHandlerSuppliers.add(() -> new HttpClientCodec());
        ConnectionFactory connectionFactory = new DefaultConnectionFactory(channelHandlerSuppliers);

        Endpoint invalidEndpoint = new Endpoint(RPC, remoteAddress, serverPort + 1);
        Assert.assertThrows(RemotingException.class, () -> {
            connectionFactory.create(invalidEndpoint);
        });

    }

}
