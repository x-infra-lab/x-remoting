package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.TestServerUtils;
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
        Connection connection1 = connectionManager.getOrCreateIfAbsent(new Endpoint(test, remoteAddress, serverPort));
        Assertions.assertNotNull(connection1);

        Connection connection2 = connectionManager.getOrCreateIfAbsent(new Endpoint(test, remoteAddress, serverPort));
        Assertions.assertTrue(connection1 == connection2);
    }

    @Test
    public void testGetOrCreateIfAbsentFail() {
        // invalid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort + 1);
        Assertions.assertThrows(RemotingException.class, () -> {
            connectionManager.getOrCreateIfAbsent(endpoint);
        });
    }

    @Test
    public void testGet() throws RemotingException {
        // valid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);

        // no connection
        Connection connection1 = connectionManager.get(endpoint);
        Assertions.assertNull(connection1);

        // create connection
        Connection connection2 = connectionManager.getOrCreateIfAbsent(endpoint);
        Assertions.assertNotNull(connection2);

        connection1 = connectionManager.get(endpoint);
        Assertions.assertNotNull(connection1);

        Assertions.assertTrue(connection1 == connection2);
    }


    @Test
    public void testGetFail() throws RemotingException {
        // invalid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort + 1);

        // no connection
        Connection connection1 = connectionManager.get(endpoint);
        Assertions.assertNull(connection1);

        // fail create connection
        Assertions.assertThrows(RemotingException.class,
                () -> {
                    connectionManager.getOrCreateIfAbsent(endpoint);
                });


        connection1 = connectionManager.get(endpoint);
        Assertions.assertNull(connection1);
    }

    @Test
    public void testCheck() throws RemotingException {
        Assertions.assertThrows(NullPointerException.class, () -> {
            connectionManager.check(null);
        });

        // valid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);
    }

    @Test
    public void testCheckWritable() throws RemotingException {
        // valid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
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

        Connection connection1 = connectionManager.get(endpoint);
        Assertions.assertNotNull(connection1);
        Assertions.assertTrue(connection1 == connection);

    }

    @Test
    public void testCheckActive() throws RemotingException {
        // valid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);

        // mock
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(false);

        Connection spyConnection = spy(connection);
        doReturn(channel).when(spyConnection).getChannel();

        ConnectionManager spyConnectionManager = spy(connectionManager);

        connectionManager.disableReconnect(endpoint);
        Assertions.assertThrows(RemotingException.class, () -> {
            spyConnectionManager.check(spyConnection);
        });

        verify(spyConnectionManager, times(1)).removeAndClose(spyConnection);
    }

    @Test
    public void testRemoveAndClose() throws RemotingException {
        // valid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);

        connectionManager.disableReconnect(endpoint);
        connectionManager.removeAndClose(connection);
        Assertions.assertNull(((ClientConnectionManager) connectionManager).connections.get(endpoint));
        Assertions.assertNull(connectionManager.get(endpoint));
        // removeAndClose again
        connectionManager.removeAndClose(connection);


        Connection mockConnection = mock(Connection.class);
        Endpoint endpoint1 = new Endpoint(test, remoteAddress, serverPort + 1);
        Assertions.assertNull(connectionManager.get(endpoint));
        doReturn(endpoint1).when(mockConnection).getEndpoint();
        connectionManager.removeAndClose(mockConnection);
        verify(mockConnection, times(1)).close();
        connectionManager.removeAndClose(mockConnection);
        verify(mockConnection, times(2)).close();

    }

    @Test
    public void testReconnect1() throws RemotingException {
        // valid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        Assertions.assertNotNull(connection);


        Map<Endpoint, ConnectionHolder> connections =
                ((ClientConnectionManager) connectionManager).getConnections();
        Assertions.assertTrue(connections.containsKey(endpoint));
        connections.remove(endpoint);

        connectionManager.reconnect(endpoint);
        Assertions.assertTrue(connections.containsKey(endpoint));
        Connection connection1 = connectionManager.get(endpoint);
        Assertions.assertNotNull(connection1);
    }

    @Test
    public void testReconnect2() throws RemotingException {
        int numPreEndpoint = 3;
        ConnectionManagerConfig connectionManagerConfig = new ConnectionManagerConfig();
        connectionManagerConfig.setConnectionNumPreEndpoint(numPreEndpoint);
        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>(), connectionManagerConfig);
        connectionManager.startup();

        // valid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);

        Map<Endpoint, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;
        ConnectionHolder connectionHolder = connections.get(endpoint);
        Assertions.assertEquals(connectionHolder.size(), numPreEndpoint);

        connectionHolder.connections.remove(connection);
        Assertions.assertEquals(connectionHolder.size(), numPreEndpoint - 1);

        connectionManager.reconnect(endpoint);
        Assertions.assertEquals(connectionHolder.size(), numPreEndpoint);
    }

    @Test
    public void testAsyncReconnect1() throws ExecutionException, InterruptedException, RemotingException {
        // valid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);

        Map<Endpoint, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;

        connectionManager = spy(connectionManager);
        Future<Void> future = connectionManager.asyncReconnect(endpoint);
        future.get();

        verify(connectionManager, times(1)).reconnect(eq(endpoint));

        Assertions.assertTrue(connections.containsKey(endpoint));
        Connection connection = connectionManager.get(endpoint);
        Assertions.assertNotNull(connection);
    }

}
