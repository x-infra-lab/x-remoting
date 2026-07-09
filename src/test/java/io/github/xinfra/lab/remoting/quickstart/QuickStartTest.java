package io.github.xinfra.lab.remoting.quickstart;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.rpc.client.RemotingClient;
import io.github.xinfra.lab.remoting.rpc.handler.EchoRequest;
import io.github.xinfra.lab.remoting.rpc.handler.RequestApi;
import io.github.xinfra.lab.remoting.rpc.server.RemotingServer;
import io.github.xinfra.lab.remoting.rpc.server.RemotingServerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class QuickStartTest {

	private static RemotingServer server;

	@BeforeAll
	public static void beforeAll() {
		RemotingServerConfig config = new RemotingServerConfig();
		config.setPort(0);
		server = new RemotingServer(config);
		server.registerRequestHandler(RequestApi.of("echo"), (EchoRequest req) -> "echo:" + req.getMsg());
		server.startup();
	}

	@AfterAll
	public static void afterAll() {
		server.shutdown();
	}

	@Test
	public void testQuickStart() throws RemotingException, InterruptedException {
		RemotingClient client = new RemotingClient();
		client.startup();
		try {
			String result = client.blockingCall(RequestApi.of("echo"), new EchoRequest("hello"),
					server.getLocalAddress(), CallOptions.defaults());
			Assertions.assertEquals("echo:hello", result);
		}
		finally {
			client.shutdown();
		}
	}

}
