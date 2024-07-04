package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.Channel;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ClientConnectionManagerTest extends ServerBase1Test {

    @Test
    public void testNewInstance() {

        Assert.assertThrows(NullPointerException.class,
                () -> {
                    new ClientConnectionManager(null);
                }
        );


        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);
    }


    @Test
    public void testGetOrCreateIfAbsent() throws RemotingException {
        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);

        Connection connection1 = connectionManager.getOrCreateIfAbsent(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort));
        Assert.assertNotNull(connection1);

        Connection connection2 = connectionManager.getOrCreateIfAbsent(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort));
        Assert.assertTrue(connection1 == connection2);
    }

    @Test
    public void testGetOrCreateIfAbsentFail() {
        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);

        Assert.assertThrows(RemotingException.class, () -> {
            connectionManager.getOrCreateIfAbsent(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort + 1));
        });
    }

    @Test
    public void testGet() throws RemotingException {
        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);

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
        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);

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
        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);

        Assert.assertThrows(RemotingException.class, () -> {
            connectionManager.check(null);
        });

        // valid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);

    }

    @Test
    public void testCheckWritable() throws RemotingException {
        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);

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
        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);

        // valid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort);
        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);

        // mock
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(false);

        Connection spyConnection = spy(connection);
        doReturn(channel).when(spyConnection).getChannel();

        Assert.assertThrows(RemotingException.class, () -> {
            connectionManager.check(spyConnection);
        });
// todo
//        Connection connection1 = connectionManager.get(endpoint);
//        Assert.assertNull(connection1);
    }

}