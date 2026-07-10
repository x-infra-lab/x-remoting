package io.github.xinfra.lab.remoting.quickstart;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.rpc.client.RemotingClient;
import io.github.xinfra.lab.remoting.rpc.handler.BlockingRequestHandler;
import io.github.xinfra.lab.remoting.rpc.handler.EchoRequest;
import io.github.xinfra.lab.remoting.rpc.server.RemotingServer;
import io.github.xinfra.lab.remoting.server.ServerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class QuickStartTest {

	private static RemotingServer server;

	@BeforeAll
	public static void beforeAll() {
		ServerConfig config = ServerConfig.builder().port(0).build();
		server = new RemotingServer(config);
		server.registerRequestHandler("echo", BlockingRequestHandler.of((EchoRequest req) -> "echo:" + req.getMsg()));
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
			String result = client.blockingCall("echo", new EchoRequest("hello"), server.getLocalAddress(),
					CallOptions.defaults());
			Assertions.assertEquals("echo:hello", result);
		}
		finally {
			client.shutdown();
		}
	}

}
