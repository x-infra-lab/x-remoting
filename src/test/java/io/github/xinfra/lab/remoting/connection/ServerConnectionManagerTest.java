package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.TestServerUtils;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class ServerConnectionManagerTest {
    private ConnectionManager connectionManager;
    private boolean skipAfter;

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
                new ServerConnectionManager();
        Assertions.assertNotNull(connectionManager);
        connectionManager.startup();
        skipAfter = false;
    }

    @AfterEach
    public void after() {
        if (!skipAfter) {
            connectionManager.shutdown();
        }
    }


    @Test
    public void testGetOrCreateIfAbsent() {
        SocketAddress socketAddress = new InetSocketAddress(remoteAddress, serverPort);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            connectionManager.getOrCreateIfAbsent(socketAddress);
        });

    }


    @Test
    public void testGet1() throws RemotingException {
        // valid socketAddress
        SocketAddress socketAddress = new InetSocketAddress(remoteAddress, serverPort);

        // no connection
        Connection connection1 = connectionManager.get(socketAddress);
        Assertions.assertNull(connection1);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            connectionManager.getOrCreateIfAbsent(socketAddress);
        });

        connection1 = connectionManager.get(socketAddress);
        Assertions.assertNull(connection1);
    }

    @Test
    public void testAdd() {
        Connection connection1 = mock(Connection.class);
        SocketAddress socketAddress1 = new InetSocketAddress( "localhost", 8080);
        doReturn(socketAddress1).when(connection1).remoteAddress();

        Connection connection2 = mock(Connection.class);
        SocketAddress socketAddress2 = new InetSocketAddress( "localhost", 8081);
        doReturn(socketAddress2).when(connection2).remoteAddress();


        connectionManager.add(connection1);
        connectionManager.add(connection2);
        Assertions.assertTrue(connection1 == connectionManager.get(socketAddress1));
        Assertions.assertTrue(connection2 == connectionManager.get(socketAddress2));

        connectionManager.removeAndClose(connection1);
        verify(connection1, times(1)).close();
        Assertions.assertNull(connectionManager.get(socketAddress1));
        Assertions.assertTrue(connection2 == connectionManager.get(socketAddress2));
    }


    @Test
    public void testShutdown() {
        Connection connection1 = mock(Connection.class);
        SocketAddress socketAddress1 = new InetSocketAddress( "localhost", 8080);
        doReturn(socketAddress1).when(connection1).remoteAddress();

        Connection connection2 = mock(Connection.class);
        SocketAddress socketAddress2 = new InetSocketAddress( "localhost", 8081);
        doReturn(socketAddress2).when(connection2).remoteAddress();


        connectionManager.add(connection1);
        connectionManager.add(connection2);

        connectionManager.shutdown();
        verify(connection1, times(1)).close();
        verify(connection2, times(1)).close();

        Assertions.assertTrue(((ServerConnectionManager) connectionManager).connections.isEmpty());
        skipAfter = true;
    }
}
