package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static io.github.xinfra.lab.remoting.connection.ServerBase1Test.remoteAddress;
import static io.github.xinfra.lab.remoting.connection.ServerBase1Test.serverPort;
import static io.github.xinfra.lab.remoting.rpc.RpcProtocol.RPC;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConnectionEventHandlerTest extends ServerBase1Test {

    @Test
    public void testChannelActive() throws Exception {
        ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler();
        ConnectionEventHandler spyHandler = spy(connectionEventHandler);

        Connection connection = mock(Connection.class);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(spyHandler);
        embeddedChannel.attr(CONNECTION).set(connection);

        embeddedChannel.pipeline().fireChannelActive();
        verify(spyHandler, times(1)).channelActive(any());
        verify(spyHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CONNECT));
    }

    @Test
    public void testChannelClose_withoutConnectionManager() throws Exception {
        ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler();
        ConnectionEventHandler spyHandler = spy(connectionEventHandler);

        Connection connection = mock(Connection.class);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(spyHandler);
        embeddedChannel.attr(CONNECTION).set(connection);

        embeddedChannel.close();
        verify(spyHandler, times(1)).close(any(), any());
        verify(connection, times(1)).onClose();
        verify(spyHandler, times(1)).channelInactive(any());
        verify(spyHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CLOSE));

    }

    @Test
    public void testChannelInactive_withoutConnectionManager() throws Exception {
        ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler();
        ConnectionEventHandler spyHandler = spy(connectionEventHandler);

        Connection connection = mock(Connection.class);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(spyHandler);
        embeddedChannel.attr(CONNECTION).set(connection);

        embeddedChannel.pipeline().fireChannelInactive();

        verify(spyHandler, times(1)).channelInactive(any());
        verify(spyHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CLOSE));
    }

    @Test
    public void testChannelExceptionCaught_withoutConnectionManager() throws Exception {
        ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler();
        ConnectionEventHandler spyHandler = spy(connectionEventHandler);

        Connection connection = mock(Connection.class);
        EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline().addLast(spyHandler);
        embeddedChannel.attr(CONNECTION).set(connection);

        embeddedChannel.pipeline().fireExceptionCaught(new RuntimeException("testChannelExceptionCaught"));
        verify(spyHandler, times(1)).exceptionCaught(any(), any());

        verify(spyHandler, times(1)).close(any(), any());
        verify(connection, times(1)).onClose();
        verify(spyHandler, times(1)).channelInactive(any());
        verify(spyHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CLOSE));

    }

//    @Test
    public void testChannelClose_withConnectionManager() throws Exception {
        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>());
        connectionManager.startup();

        connectionManager = spy(connectionManager);
        Endpoint endpoint = new Endpoint(RPC, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connection = spy(connection);
        connection.getChannel().attr(CONNECTION).set(connection);

        ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler(connectionManager);
        connectionEventHandler = spy(connectionEventHandler);

        // remove original
        connection.getChannel().pipeline().remove(ConnectionEventHandler.class);
        // add new spy instance
        connection.getChannel().pipeline().addLast(connectionEventHandler);


        ChannelFuture channelFuture = connection.close();
        channelFuture.await();
        Assert.assertTrue(channelFuture.isDone());


        verify(connectionEventHandler, times(2)).close(any(), any());
        verify(connection, times(2)).onClose();
        verify(connectionEventHandler, times(1)).channelInactive(any());
        verify(connectionManager, times(1)).removeAndClose(eq(connection));


        verify(connectionEventHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CLOSE));
        verify(connectionManager, times(1)).asyncReconnect(eq(endpoint));

    }


//    @Test
    public void testChannelInactive_withConnectionManager() throws Exception {
        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>());
        connectionManager.startup();

        connectionManager = spy(connectionManager);
        Endpoint endpoint = new Endpoint(RPC, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connection = spy(connection);
        connection.getChannel().attr(CONNECTION).set(connection);

        ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler(connectionManager);
        connectionEventHandler = spy(connectionEventHandler);

        // remove original
        connection.getChannel().pipeline().remove(ConnectionEventHandler.class);
        // add new spy instance
        connection.getChannel().pipeline().addLast(connectionEventHandler);


        ChannelFuture channelFuture = connection.getChannel().disconnect().await();
        Assert.assertTrue(channelFuture.isDone());

        // todo
        verify(connectionEventHandler, times(2)).close(any(), any());
        verify(connection, times(2)).onClose();

        verify(connectionEventHandler, times(1)).channelInactive(any());
        verify(connectionManager, times(1)).removeAndClose(eq(connection));


        verify(connectionEventHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CLOSE));
        verify(connectionManager, times(1)).asyncReconnect(eq(endpoint));

    }

//    @Test
    public void testChannelExceptionCaught_withConnectionManager() throws Exception {
        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>());
        connectionManager.startup();

        connectionManager = spy(connectionManager);
        Endpoint endpoint = new Endpoint(RPC, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connection = spy(connection);
        connection.getChannel().attr(CONNECTION).set(connection);

        ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler(connectionManager);
        connectionEventHandler = spy(connectionEventHandler);

        // remove original
        connection.getChannel().pipeline().remove(ConnectionEventHandler.class);
        // add new spy instance
        connection.getChannel().pipeline().addLast(connectionEventHandler);


        connection.getChannel().pipeline().fireExceptionCaught(new RuntimeException("testChannelExceptionCaught"));

        // todo
        TimeUnit.SECONDS.sleep(5);

        verify(connectionEventHandler, times(1)).exceptionCaught(any(), any());
        verify(connectionEventHandler, times(2)).close(any(), any());
        verify(connection, times(2)).onClose();
        verify(connectionEventHandler, times(1)).channelInactive(any());
        verify(connectionManager, times(1)).removeAndClose(eq(connection));


        verify(connectionEventHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CLOSE));
        verify(connectionManager, times(1)).asyncReconnect(eq(endpoint));

    }
}
