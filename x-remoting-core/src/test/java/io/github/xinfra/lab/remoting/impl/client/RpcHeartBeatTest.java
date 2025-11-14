package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.client.Call;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.server.RemotingServer;
import io.github.xinfra.lab.remoting.impl.server.RemotingServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;

public class RpcHeartBeatTest {

	private RemotingServer defaultRemotingServer;

	@BeforeEach
	public void before() {
		RemotingServerConfig defaultRemotingServerConfig = new RemotingServerConfig();
		defaultRemotingServerConfig.setPort(findAvailableTcpPort());
		defaultRemotingServer = new RemotingServer(defaultRemotingServerConfig);
		defaultRemotingServer.startup();
		defaultRemotingServer.registerUserProcessor(new SimpleUserProcessor());
	}

	@AfterEach
	public void after() {
		defaultRemotingServer.shutdown();
	}

	@Test
	public void heartbeatTest1() throws RemotingException, InterruptedException, IOException {
		SocketAddress remoteAddress = defaultRemotingServer.localAddress();

		Protocol protocol = new RemotingProtocol(handlerRegistry);
		ConnectionManager connectionManager = new ClientConnectionManager(protocol);
		connectionManager.startup();

		MessageFactory messageFactory = protocol.messageFactory();
		Call call = new Call(protocol);
		Message heartbeatRequestMessage = messageFactory.createHeartbeatRequestMessage();

		Connection connection = connectionManager.get(remoteAddress);

		Message heartbeatResponseMessage = call.blockingCall(heartbeatRequestMessage, connection, 1000);

		Assertions.assertNotNull(heartbeatResponseMessage);

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<Message> messageAtomicReference = new AtomicReference<>();
		call.asyncCall(heartbeatRequestMessage, connection, 1000, message -> {
			messageAtomicReference.set(message);
			countDownLatch.countDown();
		});

		countDownLatch.await(3, TimeUnit.SECONDS);
		Assertions.assertNotNull(messageAtomicReference.get());

		connectionManager.shutdown();
		protocol.close();
	}

}
