package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.SocketAddress;
import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.rpc.server.RpcServer;
import io.github.xinfra.lab.remoting.rpc.server.RpcServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;
import static io.github.xinfra.lab.remoting.rpc.RpcProtocol.RPC;

public class HeartBeatTest {

    private RpcServer rpcServer;

    @BeforeEach
    public void before() {
        RpcServerConfig rpcServerConfig = new RpcServerConfig();
        rpcServerConfig.setPort(findAvailableTcpPort());
        rpcServer = new RpcServer(rpcServerConfig);
        rpcServer.startup();
        rpcServer.registerUserProcessor(new SimpleUserProcessor());
    }

    @AfterEach
    public void after() {
        rpcServer.shutdown();
    }


    @Test
    public void heartbeatTest1() throws RemotingException, InterruptedException {
        InetSocketAddress remoteAddress = rpcServer.localAddress();

        ConnectionManager connectionManager = new ClientConnectionManager(new ConcurrentHashMap<>());
        connectionManager.startup();

        Protocol protocol = ProtocolManager.getProtocol(RPC);
        MessageFactory messageFactory = protocol.messageFactory();
        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        baseRemoting.startup();
        Message heartbeatRequestMessage = messageFactory.createHeartbeatRequestMessage();

        Connection connection = connectionManager.getOrCreateIfAbsent(new SocketAddress(RPC, remoteAddress.getHostName(), remoteAddress.getPort()));

        Message heartbeatResponseMessage = baseRemoting.syncCall(heartbeatRequestMessage, connection,
                1000);

        Assertions.assertNotNull(heartbeatResponseMessage);


        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<Message> messageAtomicReference = new AtomicReference<>();
        baseRemoting.asyncCall(heartbeatRequestMessage, connection, 1000,
                message -> {
                    messageAtomicReference.set(message);
                    countDownLatch.countDown();
                }
        );

        countDownLatch.await();
        Assertions.assertNotNull(messageAtomicReference.get());

        connectionManager.shutdown();
        baseRemoting.shutdown();
    }
}
