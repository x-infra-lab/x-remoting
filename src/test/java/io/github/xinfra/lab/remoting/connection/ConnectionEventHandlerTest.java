package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.Until;
import io.github.xinfra.lab.remoting.message.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConnectionEventHandlerTest extends ServerBase1Test {

    private static ProtocolType test = new ProtocolType("ConnectionEventHandlerTest", "ConnectionEventHandlerTest".getBytes());

    static {
        ProtocolManager.registerProtocolIfAbsent(test, new TestProtocol() {
            @Override
            public HeartbeatTrigger heartbeatTrigger() {
                return new HeartbeatTrigger() {
                    @Override
                    public void triggerHeartBeat(ChannelHandlerContext ctx) {
                        // do nothing
                    }
                };
            }
        });
    }

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

        verify(connection, times(1)).close();

    }

    @Test
    public void testChannelClose_withConnectionManager() throws Exception {
        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>());
        connectionManager.startup();

        connectionManager = spy(connectionManager);
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);
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

        ConnectionManager tempConnectionManager = connectionManager;
        Until.untilIsTrue(() -> {
            try {
                verify(tempConnectionManager, times(1)).asyncReconnect(eq(endpoint));
                return true;
            } catch (Throwable e) {
                return false;
            }
        }, 100, 30);

        verify(connectionEventHandler, times(1)).close(any(), any());
        verify(connection, times(1)).onClose();
        verify(connectionEventHandler, times(1)).channelInactive(any());
        verify(connectionManager, times(1)).removeAndClose(eq(connection));


        verify(connectionEventHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CLOSE));
        verify(connectionManager, times(1)).asyncReconnect(eq(endpoint));

        connectionManager.shutdown();
    }


    @Test
    public void testChannelInactive_withConnectionManager() throws Exception {
        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>());
        connectionManager.startup();

        connectionManager = spy(connectionManager);
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);
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

        ConnectionManager tempConnectionManager = connectionManager;
        Until.untilIsTrue(() -> {
            try {
                verify(tempConnectionManager, times(1)).asyncReconnect(eq(endpoint));
                return true;
            } catch (Throwable e) {
                return false;
            }
        }, 100, 30);

        // disconnect will call channel#close method
        verify(connectionEventHandler, times(2)).close(any(), any());
        verify(connection, times(2)).onClose();

        verify(connectionEventHandler, times(1)).channelInactive(any());
        verify(connectionManager, times(1)).removeAndClose(eq(connection));


        verify(connectionEventHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CLOSE));
        verify(connectionManager, times(1)).asyncReconnect(eq(endpoint));

        connectionManager.shutdown();
    }

    @Test
    public void testChannelExceptionCaught_withConnectionManager() throws Exception {
        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>());
        connectionManager.startup();

        connectionManager = spy(connectionManager);
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);
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

        ConnectionManager tempConnectionManager = connectionManager;
        Until.untilIsTrue(() -> {
            try {
                verify(tempConnectionManager, times(1)).asyncReconnect(eq(endpoint));
                return true;
            } catch (Throwable e) {
                return false;
            }
        }, 100, 30);

        verify(connectionEventHandler, times(1)).exceptionCaught(any(), any());
        verify(connectionEventHandler, times(1)).close(any(), any());
        verify(connection, times(1)).onClose();
        verify(connectionEventHandler, times(1)).channelInactive(any());
        verify(connectionManager, times(1)).removeAndClose(eq(connection));


        verify(connectionEventHandler, times(1)).userEventTriggered(any(), eq(ConnectionEvent.CLOSE));
        verify(connectionManager, times(1)).asyncReconnect(eq(endpoint));

        connectionManager.shutdown();
    }
}
