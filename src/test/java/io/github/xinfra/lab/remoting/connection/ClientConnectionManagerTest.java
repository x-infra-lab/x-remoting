package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.Channel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientConnectionManagerTest extends ServerBase1Test {
    ConnectionManager connectionManager;

    @Before
    public void before() {
        connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);
        connectionManager.startup();
    }

    @After
    public void after() {
        connectionManager.shutdown();

    }

    @Test
    public void testNewInstance() {

        Assert.assertThrows(NullPointerException.class,
                () -> {
                    new ClientConnectionManager(null);
                }
        );

    }


    @Test
    public void testGetOrCreateIfAbsent() throws RemotingException {

        Connection connection1 = connectionManager.getOrCreateIfAbsent(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort));
        Assert.assertNotNull(connection1);

        Connection connection2 = connectionManager.getOrCreateIfAbsent(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort));
        Assert.assertTrue(connection1 == connection2);

    }

    @Test
    public void testGetOrCreateIfAbsentFail() {

        // invalid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort + 1);
        Assert.assertThrows(RemotingException.class, () -> {
            connectionManager.getOrCreateIfAbsent(endpoint);
        });

    }

    @Test
    public void testGet() throws RemotingException {


        // valid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort);

        // no connection
        Connection connection1 = connectionManager.get(endpoint);
        Assert.assertNull(connection1);

        // create connection
        Connection connection2 = connectionManager.getOrCreateIfAbsent(endpoint);
        Assert.assertNotNull(connection2);

        connection1 = connectionManager.get(endpoint);
        Assert.assertNotNull(connection1);

        Assert.assertTrue(connection1 == connection2);

    }


    @Test
    public void testGetFail() throws RemotingException {


        // invalid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort + 1);

        // no connection
        Connection connection1 = connectionManager.get(endpoint);
        Assert.assertNull(connection1);

        // fail create connection
        Assert.assertThrows(RemotingException.class,
                () -> {
                    connectionManager.getOrCreateIfAbsent(endpoint);
                });


        connection1 = connectionManager.get(endpoint);
        Assert.assertNull(connection1);

    }

    @Test
    public void testCheck() throws RemotingException {


        Assert.assertThrows(NullPointerException.class, () -> {
            connectionManager.check(null);
        });

        // valid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);


    }

    @Test
    public void testCheckWritable() throws RemotingException {


        // valid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);

        // mock
        Channel channel = mock(Channel.class);
        when(channel.isWritable()).thenReturn(false);
        when(channel.isActive()).thenReturn(true);

        Connection spyConnection = spy(connection);
        doReturn(channel).when(spyConnection).getChannel();

        Assert.assertThrows(RemotingException.class, () -> {
            connectionManager.check(spyConnection);
        });

        Connection connection1 = connectionManager.get(endpoint);
        Assert.assertNotNull(connection1);
        Assert.assertTrue(connection1 == connection);

    }

    @Test
    public void testCheckActive() throws RemotingException {

        // valid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);

        // mock
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(false);

        Connection spyConnection = spy(connection);
        doReturn(channel).when(spyConnection).getChannel();

        ConnectionManager spyConnectionManager = spy(connectionManager);

        Assert.assertThrows(RemotingException.class, () -> {
            spyConnectionManager.check(spyConnection);
        });

        verify(spyConnectionManager, times(1)).removeAndClose(spyConnection);

    }

    @Test
    public void testRemoveAndClose() throws RemotingException {



        // valid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);

        connectionManager.removeAndClose(connection);
        Assert.assertNull(((ClientConnectionManager) connectionManager).connections.get(endpoint));
        Assert.assertNull(connectionManager.get(endpoint));
        // removeAndClose again
        connectionManager.removeAndClose(connection);


        Connection mockConnection = mock(Connection.class);
        Endpoint endpoint1 = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort + 1);
        Assert.assertNull(connectionManager.get(endpoint));
        doReturn(endpoint1).when(mockConnection).getEndpoint();
        connectionManager.removeAndClose(mockConnection);
        verify(mockConnection, times(1)).close();
        connectionManager.removeAndClose(mockConnection);
        verify(mockConnection, times(2)).close();

    }


}
