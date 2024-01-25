package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;
import static io.github.xinfra.lab.remoting.protocol.ProtocolType.RPC;

public class HeartBeatTest {

    private RpcServer rpcServer;

    @Before
    public void beforeClass() {
        rpcServer = new RpcServer(findAvailableTcpPort());
        rpcServer.startup();
        rpcServer.registerUserProcessor(new SimpleUserProcessor());

    }


    @Test
    public void heartbeatTest1() throws RemotingException, InterruptedException {
        InetSocketAddress remoteAddress = rpcServer.localAddress();


        ConnectionManager connectionManager = new ClientConnectionManager( null );
        Protocol protocol = ProtocolManager.getProtocol(ProtocolType.RPC);
        MessageFactory messageFactory = protocol.messageFactory();
        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message heartbeatRequestMessage = messageFactory.createHeartbeatRequestMessage();

        Message heartbeatResponseMessage = baseRemoting.syncCall(heartbeatRequestMessage, connectionManager.getOrCreateIfAbsent(new Endpoint(RPC, remoteAddress.getHostName(), remoteAddress.getPort())),
                1000000);

        System.out.println(heartbeatResponseMessage);
    }
}
