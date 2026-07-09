package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.rpc.client.Call;
import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.rpc.client.IDGenerator;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.client.RemotingClient;
import io.github.xinfra.lab.remoting.rpc.message.RequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.ResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;
import io.github.xinfra.lab.remoting.rpc.server.RemotingServer;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HeartbeatTest {

	private static RemotingServer remotingServer;

	private static RemotingClient remotingClient;

	private static CallOptions callOptions = CallOptions.defaults();

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
		Connection connection = remotingClient.getConnectionManager().connect(remotingServer.getLocalAddress());

		Call call = new Call() {
		};

		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		RequestMessage heartbeatRequestMessage = protocol.getMessageFactory()
			.createHeartbeatRequest(IDGenerator.nextRequestId(), SerializationType.Hession);

		CallOptions callOptions = CallOptions.defaults();
		ResponseMessage responseMessage = call.blockingCall(heartbeatRequestMessage, connection, callOptions);
		Assertions.assertEquals(responseMessage.getResponseStatus(), ResponseStatus.OK);
	}

}
