package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.TestServerUtils;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
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
    private ConnectionManager connectionManager;
    private static String remoteAddress;
    private static int serverPort;
    private static Protocol testProtocol = new TestProtocol();

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
                new ClientConnectionManager(testProtocol);
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
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
        Connection connection1 = connectionManager.getOrCreateIfAbsent(address);
        Assertions.assertNotNull(connection1);

        Connection connection2 = connectionManager.getOrCreateIfAbsent(address);
        Assertions.assertTrue(connection1 == connection2);
    }

    @Test
    public void testGetOrCreateIfAbsentFail() {
        // invalid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort + 1);
        Assertions.assertThrows(RemotingException.class, () -> {
            connectionManager.getOrCreateIfAbsent(address);
        });
    }

    @Test
    public void testGet() throws RemotingException {
        // valid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);

        // no connection
        Connection connection1 = connectionManager.get(address);
        Assertions.assertNull(connection1);

        // create connection
        Connection connection2 = connectionManager.getOrCreateIfAbsent(address);
        Assertions.assertNotNull(connection2);

        connection1 = connectionManager.get(address);
        Assertions.assertNotNull(connection1);

        Assertions.assertTrue(connection1 == connection2);
    }


    @Test
    public void testGetFail() throws RemotingException {
        // invalid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort + 1);

        // no connection
        Connection connection1 = connectionManager.get(address);
        Assertions.assertNull(connection1);

        // fail create connection
        Assertions.assertThrows(RemotingException.class,
                () -> {
                    connectionManager.getOrCreateIfAbsent(address);
                });


        connection1 = connectionManager.get(address);
        Assertions.assertNull(connection1);
    }

    @Test
    public void testCheck() throws RemotingException {
        Assertions.assertThrows(NullPointerException.class, () -> {
            connectionManager.check(null);
        });

        // valid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(address);
        connectionManager.check(connection);
    }

    @Test
    public void testCheckWritable() throws RemotingException {
        // valid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(address);
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

        Connection connection1 = connectionManager.get(address);
        Assertions.assertNotNull(connection1);
        Assertions.assertTrue(connection1 == connection);

    }

    @Test
    public void testCheckActive() throws RemotingException {
        // valid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(address);
        connectionManager.check(connection);

        // mock
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(false);

        Connection spyConnection = spy(connection);
        doReturn(channel).when(spyConnection).getChannel();

        ConnectionManager spyConnectionManager = spy(connectionManager);

        connectionManager.disableReconnect(address);
        Assertions.assertThrows(RemotingException.class, () -> {
            spyConnectionManager.check(spyConnection);
        });

        verify(spyConnectionManager, times(1)).removeAndClose(eq(spyConnection));
    }

    @Test
    public void testRemoveAndClose() throws RemotingException {
        // valid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(address);

        connectionManager.disableReconnect(address);
        connectionManager.removeAndClose(connection);
        Assertions.assertNull(((ClientConnectionManager) connectionManager).connections.get(address));
        Assertions.assertNull(connectionManager.get(address));
        // removeAndClose again
        connectionManager.removeAndClose(connection);


        Connection mockConnection = mock(Connection.class);
        // invalid
        InetSocketAddress invalidAddress = new InetSocketAddress(remoteAddress, serverPort + 1);
        Assertions.assertNull(connectionManager.get(invalidAddress));
        doReturn(invalidAddress).when(mockConnection).remoteAddress();
        connectionManager.removeAndClose(mockConnection);
        verify(mockConnection, times(1)).close();
        connectionManager.removeAndClose(mockConnection);
        verify(mockConnection, times(2)).close();

    }

    @Test
    public void testReconnect1() throws RemotingException {
        // valid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(address);
        Assertions.assertNotNull(connection);


        Map<SocketAddress, ConnectionHolder> connections =
                ((ClientConnectionManager) connectionManager).connections;
        Assertions.assertTrue(connections.containsKey(address));
        connections.remove(address);

        connectionManager.reconnect(address);
        Assertions.assertTrue(connections.containsKey(address));
        Connection connection1 = connectionManager.get(address);
        Assertions.assertNotNull(connection1);
    }

    @Test
    public void testReconnect2() throws RemotingException {
        int numPreEndpoint = 3;
        ConnectionManagerConfig connectionManagerConfig = new ConnectionManagerConfig();
        connectionManagerConfig.setConnectionNumPreEndpoint(numPreEndpoint);
        ConnectionManager connectionManager = new ClientConnectionManager(testProtocol, connectionManagerConfig);
        connectionManager.startup();

        // valid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(address);

        Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;
        ConnectionHolder connectionHolder = connections.get(address);
        Assertions.assertEquals(connectionHolder.size(), numPreEndpoint);

        connectionHolder.connections.remove(connection);
        Assertions.assertEquals(connectionHolder.size(), numPreEndpoint - 1);

        connectionManager.reconnect(address);
        Assertions.assertEquals(connectionHolder.size(), numPreEndpoint);
    }

    @Test
    public void testAsyncReconnect1() throws ExecutionException, InterruptedException, RemotingException {
        // valid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);

        Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;

        connectionManager = spy(connectionManager);
        Future<Void> future = connectionManager.asyncReconnect(address);
        future.get();

        verify(connectionManager, times(1)).reconnect(eq(address));

        Assertions.assertTrue(connections.containsKey(address));
        Connection connection = connectionManager.get(address);
        Assertions.assertNotNull(connection);
    }

    @Test
    public void testAsyncReconnect2() throws InterruptedException, RemotingException, TimeoutException, UnknownHostException {
        // valid socketAddress
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
        Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;

        Connection connection = connectionManager.getOrCreateIfAbsent(address);
        connectionManager.removeAndClose(connection);
        Assertions.assertTrue(!connections.containsKey(address));

        Wait.untilIsTrue(
                () -> {
                    ConnectionHolder connectionHolder = connections.get(address);
                    if (connectionHolder != null && connectionHolder.get() != null) {
                        return true;
                    }
                    return false;
                }, 100, 30
        );

        Assertions.assertTrue(connections.containsKey(address));
        connection = connectionManager.get(address);
        Assertions.assertNotNull(connection);
    }

    @Test
    void testDisableReconnect() throws RemotingException, ExecutionException, InterruptedException, TimeoutException {
        InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);

        connectionManager.reconnect(address);
        connectionManager.asyncReconnect(address).get(3, TimeUnit.SECONDS);

        connectionManager.disableReconnect(address);
        Assertions.assertThrowsExactly(RemotingException.class, () -> {
            connectionManager.reconnect(address);
        });

        ExecutionException executionException = Assertions.assertThrows(ExecutionException.class, () -> {
            connectionManager.asyncReconnect(address).get(3, TimeUnit.SECONDS);
        });
        Assertions.assertTrue(executionException.getCause() instanceof RemotingException);

        connectionManager.enableReconnect(address);

        connectionManager.reconnect(address);
        connectionManager.asyncReconnect(address).get(3, TimeUnit.SECONDS);
    }
}
