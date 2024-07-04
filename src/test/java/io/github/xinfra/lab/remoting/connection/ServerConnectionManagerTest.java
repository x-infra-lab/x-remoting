package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.junit.Assert;
import org.junit.Test;


public class ServerConnectionManagerTest extends ServerBase1Test {

    @Test
    public void testNewInstance() {

        ConnectionManager connectionManager =
                new ServerConnectionManager();
        Assert.assertNotNull(connectionManager);
    }


    @Test
    public void testGetOrCreateIfAbsent() {
        ConnectionManager connectionManager =
                new ServerConnectionManager();
        Assert.assertNotNull(connectionManager);

        Assert.assertThrows(UnsupportedOperationException.class, () -> {
            connectionManager.getOrCreateIfAbsent(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort));
        });
    }


    @Test
    public void testGet1() throws RemotingException {
        ConnectionManager connectionManager =
                new ServerConnectionManager();
        Assert.assertNotNull(connectionManager);

        // valid endpoint
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, remoteAddress, serverPort);

        // no connection
        Connection connection1 = connectionManager.get(endpoint);
        Assert.assertNull(connection1);

        Assert.assertThrows(UnsupportedOperationException.class, () -> {
            connectionManager.getOrCreateIfAbsent(endpoint);
        });

        connection1 = connectionManager.get(endpoint);
        Assert.assertNull(connection1);

    }

}
