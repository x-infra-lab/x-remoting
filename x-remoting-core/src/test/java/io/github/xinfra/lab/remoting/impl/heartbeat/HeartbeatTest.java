package io.github.xinfra.lab.remoting.impl.heartbeat;

import io.github.xinfra.lab.remoting.client.Call;
import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.client.RemotingClient;
import io.github.xinfra.lab.remoting.impl.server.RemotingServer;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HeartbeatTest {

	private static RemotingServer remotingServer;

	private static RemotingClient remotingClient;

	private static CallOptions callOptions = new CallOptions();

	@BeforeAll
	public static void beforeAll() {
		remotingServer = new RemotingServer();
		remotingServer.startup();

		remotingClient = new RemotingClient();
		remotingClient.startup();
	}

	@AfterAll
	public static void afterAll() {
		remotingServer.shutdown();
		remotingClient.shutdown();
	}

	@Test
	public void testHeartbeat() throws RemotingException, InterruptedException {
		Connection connection = remotingClient.getConnectionManager().connect(remotingServer.localAddress());
		remotingClient.getConnectionManager().heartbeater().disableHeartBeat(connection);

		Call call = new Call() {
		};

		Protocol protocol = connection.getProtocol();
		RequestMessage heartbeatRequestMessage = protocol.messageFactory()
			.createHeartbeatRequest(IDGenerator.nextRequestId(), SerializationType.Hession);

		CallOptions callOptions = new CallOptions();
		ResponseMessage responseMessage = call.blockingCall(heartbeatRequestMessage, connection, callOptions);
		Assertions.assertEquals(responseMessage.responseStatus(), ResponseStatus.OK);
	}

}
