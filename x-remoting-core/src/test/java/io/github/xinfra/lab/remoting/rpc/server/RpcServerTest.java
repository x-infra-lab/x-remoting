package io.github.xinfra.lab.remoting.rpc.server;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.rpc.client.RpcClient;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeCallBack;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeFuture;
import io.github.xinfra.lab.remoting.rpc.client.SimpleRequest;
import io.github.xinfra.lab.remoting.rpc.client.SimpleUserProcessor;
import io.github.xinfra.lab.remoting.server.BaseRemotingServer;
import io.github.xinfra.lab.remoting.server.RemotingServerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class RpcServerTest {

	private static RpcServer rpcServer;

	private static RpcClient rpcClient;

	@BeforeAll
	public static void beforeAll() {
		rpcClient = new RpcClient();
		rpcClient.startup();
		rpcClient.registerUserProcessor(new SimpleUserProcessor());

		RpcServerConfig config = new RpcServerConfig();
		config.setManageConnection(true);
		rpcServer = new RpcServer(config);
		rpcServer.startup();
		rpcServer.registerUserProcessor(new SimpleUserProcessor());
	}

	@AfterAll
	public static void afterAll() {
		rpcClient.shutdown();

		rpcServer.shutdown();
	}

	@Test
	public void testSyncCall() throws RemotingException, InterruptedException {
		SocketAddress serverAddress = rpcServer.localAddress();
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);
		String result = rpcClient.syncCall(request, serverAddress, 1000);
		Assertions.assertEquals(result, "echo:" + msg);

		Connection connection = rpcClient.getConnectionManager().get(serverAddress);
		result = rpcServer.syncCall(request, connection.getChannel().localAddress(), 1000);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testAsyncCall1() throws RemotingException, InterruptedException, TimeoutException {
		SocketAddress serverAddress = rpcServer.localAddress();
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);
		RpcInvokeFuture<String> future = rpcClient.asyncCall(request, serverAddress, 1000);

		String result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);

		Connection connection = rpcClient.getConnectionManager().get(serverAddress);
		future = rpcServer.asyncCall(request, connection.getChannel().localAddress(), 1000);
		result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testAsyncCall2() throws RemotingException, InterruptedException, TimeoutException {
		SocketAddress serverAddress = rpcServer.localAddress();
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> result = new AtomicReference<>();
		rpcClient.asyncCall(request, serverAddress, 1000, new RpcInvokeCallBack<String>() {
			@Override
			public void onException(Throwable t) {
				countDownLatch.countDown();
			}

			@Override
			public void onResponse(String response) {
				result.set(response);
				countDownLatch.countDown();
			}
		});

		countDownLatch.await(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result.get(), "echo:" + msg);

		Connection connection = rpcClient.getConnectionManager().get(serverAddress);
		CountDownLatch countDownLatch2 = new CountDownLatch(1);
		AtomicReference<String> result2 = new AtomicReference<>();
		rpcServer.asyncCall(request, connection.getChannel().localAddress(), 1000, new RpcInvokeCallBack<String>() {
			@Override
			public void onException(Throwable t) {
				countDownLatch2.countDown();
			}

			@Override
			public void onResponse(String response) {
				result2.set(response);
				countDownLatch2.countDown();
			}
		});

		countDownLatch2.await(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result2.get(), "echo:" + msg);
	}

	@Test
	public void testOnewayCall() throws RemotingException, InterruptedException {
		SocketAddress serverAddress = rpcServer.localAddress();

		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);

		rpcClient.oneway(request, serverAddress);

		Connection connection = rpcClient.getConnectionManager().get(serverAddress);
		rpcServer.oneway(request, connection.getChannel().localAddress());

		TimeUnit.SECONDS.sleep(2);
	}

	@Test
	public void testRegisterUserProcessor() throws RemotingException, InterruptedException, TimeoutException {
		testProtocol = spy(testProtocol);
		MessageHandler messageHandler = mock(MessageHandler.class);
		doReturn(messageHandler).when(testProtocol).messageHandler();

		RemotingServerConfig config = new RemotingServerConfig();
		config.setPort(findAvailableTcpPort());
		config.setManageConnection(true);

		BaseRemotingServer server = new BaseRemotingServer(config) {
			@Override
			public Protocol protocol() {
				return testProtocol;
			}
		};

		server.startup();

		UserProcessor<String> userProcessor1 = new UserProcessor<String>() {
			@Override
			public String interest() {
				return String.class.getName();
			}

			@Override
			public Object handRequest(String request) {
				// do nothing
				return null;
			}
		};

		server.registerUserProcessor(userProcessor1);

		verify(messageHandler, times(1)).registerUserProcessor(eq(userProcessor1));
	}

}
