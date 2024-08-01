package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.SocketAddress;
import io.github.xinfra.lab.remoting.common.TestServerUtils;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientConnectionManagerTest {
    ConnectionManager connectionManager;
    private ProtocolType test = new ProtocolType("ClientConnectionManagerTest", "ClientConnectionManagerTest".getBytes());

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

    @BeforeEach
    public void before() {
        connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assertions.assertNotNull(connectionManager);
        connectionManager.startup();
    }

    @AfterEach
    public void after() {
        connectionManager.shutdown();
    }

    @Test
    public void testNewInstance() {
        Assertions.assertThrows(NullPointerException.class,
                () -> {
                    new ClientConnectionManager(null);
                }
        );
    }


    @Test
    public void testGetOrCreateIfAbsent() throws RemotingException {
        Connection connection1 = connectionManager.getOrCreateIfAbsent(new SocketAddress(test, remoteAddress, serverPort));
        Assertions.assertNotNull(connection1);

        Connection connection2 = connectionManager.getOrCreateIfAbsent(new SocketAddress(test, remoteAddress, serverPort));
        Assertions.assertTrue(connection1 == connection2);
    }

    @Test
    public void testGetOrCreateIfAbsentFail() {
        // invalid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort + 1);
        Assertions.assertThrows(RemotingException.class, () -> {
            connectionManager.getOrCreateIfAbsent(socketAddress);
        });
    }

    @Test
    public void testGet() throws RemotingException {
        // valid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);

        // no connection
        Connection connection1 = connectionManager.get(socketAddress);
        Assertions.assertNull(connection1);

        // create connection
        Connection connection2 = connectionManager.getOrCreateIfAbsent(socketAddress);
        Assertions.assertNotNull(connection2);

        connection1 = connectionManager.get(socketAddress);
        Assertions.assertNotNull(connection1);

        Assertions.assertTrue(connection1 == connection2);
    }


    @Test
    public void testGetFail() throws RemotingException {
        // invalid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort + 1);

        // no connection
        Connection connection1 = connectionManager.get(socketAddress);
        Assertions.assertNull(connection1);

        // fail create connection
        Assertions.assertThrows(RemotingException.class,
                () -> {
                    connectionManager.getOrCreateIfAbsent(socketAddress);
                });


        connection1 = connectionManager.get(socketAddress);
        Assertions.assertNull(connection1);
    }

    @Test
    public void testCheck() throws RemotingException {
        Assertions.assertThrows(NullPointerException.class, () -> {
            connectionManager.check(null);
        });

        // valid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(socketAddress);
        connectionManager.check(connection);
    }

    @Test
    public void testCheckWritable() throws RemotingException {
        // valid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(socketAddress);
        connectionManager.check(connection);

        // mock
        Channel channel = mock(Channel.class);
        when(channel.isWritable()).thenReturn(false);
        when(channel.isActive()).thenReturn(true);

        Connection spyConnection = spy(connection);
        doReturn(channel).when(spyConnection).getChannel();

        Assertions.assertThrows(RemotingException.class, () -> {
            connectionManager.check(spyConnection);
        });

        Connection connection1 = connectionManager.get(socketAddress);
        Assertions.assertNotNull(connection1);
        Assertions.assertTrue(connection1 == connection);

    }

    @Test
    public void testCheckActive() throws RemotingException {
        // valid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(socketAddress);
        connectionManager.check(connection);

        // mock
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(false);

        Connection spyConnection = spy(connection);
        doReturn(channel).when(spyConnection).getChannel();

        ConnectionManager spyConnectionManager = spy(connectionManager);

        connectionManager.disableReconnect(socketAddress);
        Assertions.assertThrows(RemotingException.class, () -> {
            spyConnectionManager.check(spyConnection);
        });

        verify(spyConnectionManager, times(1)).removeAndClose(spyConnection);
    }

    @Test
    public void testRemoveAndClose() throws RemotingException {
        // valid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(socketAddress);

        connectionManager.disableReconnect(socketAddress);
        connectionManager.removeAndClose(connection);
        Assertions.assertNull(((ClientConnectionManager) connectionManager).connections.get(socketAddress));
        Assertions.assertNull(connectionManager.get(socketAddress));
        // removeAndClose again
        connectionManager.removeAndClose(connection);


        Connection mockConnection = mock(Connection.class);
        SocketAddress socketAddress1 = new SocketAddress(test, remoteAddress, serverPort + 1);
        Assertions.assertNull(connectionManager.get(socketAddress));
        doReturn(socketAddress1).when(mockConnection).getSocketAddress();
        connectionManager.removeAndClose(mockConnection);
        verify(mockConnection, times(1)).close();
        connectionManager.removeAndClose(mockConnection);
        verify(mockConnection, times(2)).close();

    }

    @Test
    public void testReconnect1() throws RemotingException {
        // valid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(socketAddress);
        Assertions.assertNotNull(connection);


        Map<SocketAddress, ConnectionHolder> connections =
                ((ClientConnectionManager) connectionManager).connections;
        Assertions.assertTrue(connections.containsKey(socketAddress));
        connections.remove(socketAddress);

        connectionManager.reconnect(socketAddress);
        Assertions.assertTrue(connections.containsKey(socketAddress));
        Connection connection1 = connectionManager.get(socketAddress);
        Assertions.assertNotNull(connection1);
    }

    @Test
    public void testReconnect2() throws RemotingException {
        int numPreEndpoint = 3;
        ConnectionManagerConfig connectionManagerConfig = new ConnectionManagerConfig();
        connectionManagerConfig.setConnectionNumPreEndpoint(numPreEndpoint);
        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>(), connectionManagerConfig);
        connectionManager.startup();

        // valid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(socketAddress);

        Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;
        ConnectionHolder connectionHolder = connections.get(socketAddress);
        Assertions.assertEquals(connectionHolder.size(), numPreEndpoint);

        connectionHolder.connections.remove(connection);
        Assertions.assertEquals(connectionHolder.size(), numPreEndpoint - 1);

        connectionManager.reconnect(socketAddress);
        Assertions.assertEquals(connectionHolder.size(), numPreEndpoint);
    }

    @Test
    public void testAsyncReconnect1() throws ExecutionException, InterruptedException, RemotingException {
        // valid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);

        Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;

        connectionManager = spy(connectionManager);
        Future<Void> future = connectionManager.asyncReconnect(socketAddress);
        future.get();

        verify(connectionManager, times(1)).reconnect(eq(socketAddress));

        Assertions.assertTrue(connections.containsKey(socketAddress));
        Connection connection = connectionManager.get(socketAddress);
        Assertions.assertNotNull(connection);
    }

    @Test
    public void testAsyncReconnect2() throws InterruptedException, RemotingException, TimeoutException {
        // valid socketAddress
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);

        Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;

        Connection connection = connectionManager.getOrCreateIfAbsent(socketAddress);
        connectionManager.removeAndClose(connection);
        Assertions.assertTrue(!connections.containsKey(socketAddress));

        Wait.untilIsTrue(
                () -> {
                    ConnectionHolder connectionHolder = connections.get(socketAddress);
                    if (connectionHolder != null && connectionHolder.get() != null) {
                        return true;
                    }
                    return false;
                }, 100, 30
        );

        Assertions.assertTrue(connections.containsKey(socketAddress));
        connection = connectionManager.get(socketAddress);
        System.out.println("connections:" + connections);
        Assertions.assertNotNull(connection);
    }

    @Test
    void testDisableReconnect() throws RemotingException, ExecutionException, InterruptedException, TimeoutException {
        SocketAddress socketAddress = new SocketAddress(test, remoteAddress, serverPort);

        connectionManager.reconnect(socketAddress);
        connectionManager.asyncReconnect(socketAddress).get(3, TimeUnit.SECONDS);

        connectionManager.disableReconnect(socketAddress);
        RemotingException remotingException = Assertions.assertThrowsExactly(RemotingException.class, () -> {
            connectionManager.reconnect(socketAddress);
        });

        ExecutionException executionException = Assertions.assertThrows(ExecutionException.class, () -> {
            connectionManager.asyncReconnect(socketAddress).get(3, TimeUnit.SECONDS);
        });
        Assertions.assertTrue(executionException.getCause() instanceof RemotingException);

        connectionManager.enableReconnect(socketAddress);

        connectionManager.reconnect(socketAddress);
        connectionManager.asyncReconnect(socketAddress).get(3, TimeUnit.SECONDS);
    }
}
