package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


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
        List<ChannelHandler> channelHandlers = new ArrayList<>();
        channelHandlers.add(new HttpClientCodec());
        ConnectionFactory connectionFactory = new DefaultConnectionFactory(channelHandlers);

        Connection connection = connectionFactory.create(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort));
        Assert.assertNotNull(connection);

        Channel channel = connection.getChannel();
        Assert.assertNotNull(channel);
        Assert.assertTrue(channel.isActive());

        Collection<ChannelHandler> handlers = channel.pipeline().toMap().values();
        Assert.assertTrue(handlers.containsAll(channelHandlers));
        Assert.assertEquals(((InetSocketAddress) channel.remoteAddress()).getHostName(), remoteAddress);

    }

    @Test
    public void testCreateFail()  {
        List<ChannelHandler> channelHandlers = new ArrayList<>();
        channelHandlers.add(new HttpClientCodec());
        ConnectionFactory connectionFactory = new DefaultConnectionFactory(channelHandlers);

        Assert.assertThrows(RemotingException.class, () -> {
            connectionFactory.create(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort + 1));
        });

    }

}
