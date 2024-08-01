package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.SocketAddress;
import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static io.github.xinfra.lab.remoting.connection.Connection.HEARTBEAT_FAIL_COUNT;
import static io.github.xinfra.lab.remoting.connection.Connection.PROTOCOL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ConnectionTest {
    private Connection connection;

    private ProtocolType test = new ProtocolType("ConnectionTest", "ConnectionTest".getBytes());

    @BeforeEach
    public void before() {
        SocketAddress socketAddress = new SocketAddress(test, "localhost", 0);
        Channel channel = new EmbeddedChannel();
        connection = new Connection(socketAddress, channel);
    }

    @Test
    public void testNewInstance() {
        SocketAddress socketAddress = new SocketAddress(test, "localhost", 0);
        Channel channel = new EmbeddedChannel();
        Connection connection = new Connection(socketAddress, channel);

        Assertions.assertNotNull(connection);
        Assertions.assertEquals(connection.getChannel(), channel);
        Assertions.assertEquals(connection.getSocketAddress(), socketAddress);
        Assertions.assertEquals(connection.remoteAddress(), channel.remoteAddress());
        Assertions.assertEquals(connection.getChannel().attr(PROTOCOL).get(), socketAddress.getProtocolType());
        Assertions.assertEquals(connection.getChannel().attr(CONNECTION).get(), connection);
        Assertions.assertEquals((long) connection.getChannel().attr(HEARTBEAT_FAIL_COUNT).get(), 0L);
    }

    @Test
    public void testConnectionWithInvokeFuture() {

        final int requestId1 = IDGenerator.nextRequestId();
        Assertions.assertNull(connection.removeInvokeFuture(requestId1));

        connection.addInvokeFuture(new InvokeFuture(requestId1, connection.getProtocol()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            connection.addInvokeFuture(new InvokeFuture(requestId1, connection.getProtocol()));
        });


        final int requestId2 = IDGenerator.nextRequestId();
        InvokeFuture invokeFuture = new InvokeFuture(requestId2, connection.getProtocol());
        connection.addInvokeFuture(invokeFuture);

        Assertions.assertEquals(invokeFuture, connection.removeInvokeFuture(requestId2));
        Assertions.assertNull(connection.removeInvokeFuture(requestId2));
        Assertions.assertNull(connection.removeInvokeFuture(requestId2));
    }

    @Test
    public void testCloseConnection() throws InterruptedException {
        connection.close().sync();
        Assertions.assertFalse(connection.getChannel().isActive());

        connection.close().sync();
        Assertions.assertFalse(connection.getChannel().isActive());
    }

    @Test
    public void testOnCloseConnection() {
        int times = 10;
        List<Integer> requestIds = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            Integer requestId = IDGenerator.nextRequestId();
            requestIds.add(requestId);
            connection.addInvokeFuture(new InvokeFuture(requestId, connection.getProtocol()));
        }
        Assertions.assertEquals(requestIds.size(), times);


        MessageFactory messageFactory = mock(MessageFactory.class);
        Message message = mock(Message.class);
        doReturn(message).when(messageFactory).createConnectionClosedMessage(anyInt(), any());
        doReturn(test).when(message).protocolType();
        ProtocolManager.registerProtocolIfAbsent(test, new TestProtocol() {
            @Override
            public MessageFactory messageFactory() {
                return messageFactory;
            }

            @Override
            public MessageHandler messageHandler() {
                return new MessageHandler() {
                    @Override
                    public Executor executor() {
                        return Executors.newSingleThreadExecutor();
                    }

                    @Override
                    public void handleMessage(RemotingContext remotingContext, Object msg) {

                    }
                };
            }
        });
        connection.onClose();

        Assertions.assertEquals(0, connection.invokeMap.size());
    }
}
