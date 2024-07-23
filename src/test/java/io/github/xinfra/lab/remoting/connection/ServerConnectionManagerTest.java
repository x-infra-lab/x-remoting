package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class ServerConnectionManagerTest extends ServerBase1Test {
    private ConnectionManager connectionManager;
    private boolean skipAfter;
    private ProtocolType test = new ProtocolType("ServerConnectionManagerTest", "ServerConnectionManagerTest".getBytes());

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
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            connectionManager.getOrCreateIfAbsent(new Endpoint(test, remoteAddress, serverPort));
        });

    }


    @Test
    public void testGet1() throws RemotingException {
        // valid endpoint
        Endpoint endpoint = new Endpoint(test, remoteAddress, serverPort);

        // no connection
        Connection connection1 = connectionManager.get(endpoint);
        Assertions.assertNull(connection1);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            connectionManager.getOrCreateIfAbsent(endpoint);
        });

        connection1 = connectionManager.get(endpoint);
        Assertions.assertNull(connection1);
    }

    @Test
    public void testAdd() {
        Connection connection1 = mock(Connection.class);
        Endpoint endpoint1 = new Endpoint(test, "localhost", 8080);
        doReturn(endpoint1).when(connection1).getEndpoint();

        Connection connection2 = mock(Connection.class);
        Endpoint endpoint2 = new Endpoint(test, "localhost", 8081);
        doReturn(endpoint2).when(connection2).getEndpoint();


        connectionManager.add(connection1);
        connectionManager.add(connection2);
        Assertions.assertTrue(connection1 == connectionManager.get(endpoint1));
        Assertions.assertTrue(connection2 == connectionManager.get(endpoint2));

        connectionManager.removeAndClose(connection1);
        verify(connection1, times(1)).close();
        Assertions.assertNull(connectionManager.get(endpoint1));
        Assertions.assertTrue(connection2 == connectionManager.get(endpoint2));
    }


    @Test
    public void testShutdown() {
        Connection connection1 = mock(Connection.class);
        Endpoint endpoint1 = new Endpoint(test, "localhost", 8080);
        doReturn(endpoint1).when(connection1).getEndpoint();

        Connection connection2 = mock(Connection.class);
        Endpoint endpoint2 = new Endpoint(test, "localhost", 8081);
        doReturn(endpoint2).when(connection2).getEndpoint();


        connectionManager.add(connection1);
        connectionManager.add(connection2);

        connectionManager.shutdown();
        verify(connection1, times(1)).close();
        verify(connection2, times(1)).close();

        Assertions.assertNull(connectionManager.get(endpoint1));
        Assertions.assertNull(connectionManager.get(endpoint2));
        skipAfter = true;
    }
}
