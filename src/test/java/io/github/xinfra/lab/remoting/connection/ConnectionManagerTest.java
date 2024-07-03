package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;


public class ConnectionManagerTest extends ServerBase1Test {


    @Test
    public void testNewInstance_Client() {

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
    public void testGetOrCreateIfAbsent_Client() throws RemotingException {
        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);

        Connection connection1 = connectionManager.getOrCreateIfAbsent(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort));
        Assert.assertNotNull(connection1);

        Connection connection2 = connectionManager.getOrCreateIfAbsent(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort));
        Assert.assertTrue(connection1 == connection2);
    }

    @Test
    public void testGetOrCreateIfAbsentFail_Client() {
        ConnectionManager connectionManager =
                new ClientConnectionManager(new ConcurrentHashMap<>());
        Assert.assertNotNull(connectionManager);

        Assert.assertThrows(RemotingException.class, () -> {
            connectionManager.getOrCreateIfAbsent(new Endpoint(ProtocolType.RPC, remoteAddress, serverPort + 1));
        });
    }



    @Test
    public void testGet_Client() {
       // todo
    }
}
