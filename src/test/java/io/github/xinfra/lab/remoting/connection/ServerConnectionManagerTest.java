package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static io.github.xinfra.lab.remoting.rpc.RpcProtocol.RPC;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class ServerConnectionManagerTest extends ServerBase1Test {
    private ConnectionManager connectionManager;
    private boolean skipAfter;

    @Before
    public void before() {
        connectionManager =
                new ServerConnectionManager();
        Assert.assertNotNull(connectionManager);
        connectionManager.startup();
        skipAfter = false;
    }

    @After
    public void after() {
        if (!skipAfter) {
            connectionManager.shutdown();
        }
    }


    @Test
    public void testGetOrCreateIfAbsent() {


        Assert.assertThrows(UnsupportedOperationException.class, () -> {
            connectionManager.getOrCreateIfAbsent(new Endpoint(RPC, remoteAddress, serverPort));
        });

    }


    @Test
    public void testGet1() throws RemotingException {


        // valid endpoint
        Endpoint endpoint = new Endpoint(RPC, remoteAddress, serverPort);

        // no connection
        Connection connection1 = connectionManager.get(endpoint);
        Assert.assertNull(connection1);

        Assert.assertThrows(UnsupportedOperationException.class, () -> {
            connectionManager.getOrCreateIfAbsent(endpoint);
        });

        connection1 = connectionManager.get(endpoint);
        Assert.assertNull(connection1);

    }

    @Test
    public void testAdd() {
        Connection connection1 = mock(Connection.class);
        Endpoint endpoint1 = new Endpoint(RPC, "localhost", 8080);
        doReturn(endpoint1).when(connection1).getEndpoint();

        Connection connection2 = mock(Connection.class);
        Endpoint endpoint2 = new Endpoint(RPC, "localhost", 8081);
        doReturn(endpoint2).when(connection2).getEndpoint();


        connectionManager.add(connection1);
        connectionManager.add(connection2);
        Assert.assertTrue(connection1 == connectionManager.get(endpoint1));
        Assert.assertTrue(connection2 == connectionManager.get(endpoint2));

        connectionManager.removeAndClose(connection1);
        verify(connection1, times(1)).close();
        Assert.assertNull(connectionManager.get(endpoint1));
        Assert.assertTrue(connection2 == connectionManager.get(endpoint2));
    }


    @Test
    public void testShutdown() {
        Connection connection1 = mock(Connection.class);
        Endpoint endpoint1 = new Endpoint(RPC, "localhost", 8080);
        doReturn(endpoint1).when(connection1).getEndpoint();

        Connection connection2 = mock(Connection.class);
        Endpoint endpoint2 = new Endpoint(RPC, "localhost", 8081);
        doReturn(endpoint2).when(connection2).getEndpoint();


        connectionManager.add(connection1);
        connectionManager.add(connection2);

        connectionManager.shutdown();
        verify(connection1, times(1)).close();
        verify(connection2, times(1)).close();

        Assert.assertNull(connectionManager.get(endpoint1));
        Assert.assertNull(connectionManager.get(endpoint2));
        skipAfter = true;
    }
}
