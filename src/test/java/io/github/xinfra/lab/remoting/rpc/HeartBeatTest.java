package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.client.InvokeCallBack;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

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

        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>());
        Protocol protocol = ProtocolManager.getProtocol(ProtocolType.RPC);
        MessageFactory messageFactory = protocol.messageFactory();
        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message heartbeatRequestMessage = messageFactory.createHeartbeatRequestMessage();

        Connection connection = connectionManager.getOrCreateIfAbsent(new Endpoint(RPC, remoteAddress.getHostName(), remoteAddress.getPort()));

        Message heartbeatResponseMessage = baseRemoting.syncCall(heartbeatRequestMessage, connection,
                1000);

        Assert.assertNotNull(heartbeatResponseMessage);


        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<Message> messageAtomicReference = new AtomicReference<>();
        baseRemoting.asyncCall(heartbeatRequestMessage, connection, 1000,
                message -> {
                    countDownLatch.countDown();
                    messageAtomicReference.set(message);
                }
        );

        countDownLatch.await();
        Assert.assertNotNull(messageAtomicReference.get());
    }
}
