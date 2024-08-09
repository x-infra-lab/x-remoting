package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.server.RpcServer;
import io.github.xinfra.lab.remoting.rpc.server.RpcServerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.SocketAddress;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;

public class RpcTest {

	private static RpcServer rpcServer;

	private static RpcClient rpcClient;

	@BeforeAll
	public static void beforeClass() {
		RpcServerConfig rpcServerConfig = new RpcServerConfig();
		rpcServerConfig.setPort(findAvailableTcpPort());
		rpcServer = new RpcServer(rpcServerConfig);
		rpcServer.startup();
		rpcServer.registerUserProcessor(new SimpleUserProcessor());

		rpcClient = new RpcClient();
		rpcClient.startup();
	}

	@AfterAll
	public static void afterClass() {
		rpcServer.shutdown();
		rpcClient.shutdown();
	}

	@Test
	public void testBasicCall1() {
		SocketAddress remoteAddress = rpcServer.localAddress();

		try {
			String result = rpcClient.syncCall(new SimpleRequest("test"), remoteAddress, 1000);
			Assertions.assertEquals("echo:test", result);

			result = rpcClient.syncCall(new SimpleRequest("test"), remoteAddress, 1000);
			Assertions.assertEquals("echo:test", result);

			result = rpcClient.syncCall(new SimpleRequest("test"), remoteAddress, 1000);
			Assertions.assertEquals("echo:test", result);

		}
		catch (RemotingException e) {
			Assertions.fail(e.getMessage());
		}
		catch (InterruptedException e) {
			Assertions.fail(e.getMessage());
		}

	}

}
