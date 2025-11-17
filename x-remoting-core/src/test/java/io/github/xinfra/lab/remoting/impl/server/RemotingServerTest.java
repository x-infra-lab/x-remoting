package io.github.xinfra.lab.remoting.impl.server;

import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.client.RemotingClient;
import io.github.xinfra.lab.remoting.impl.client.RemotingCallBack;
import io.github.xinfra.lab.remoting.impl.client.RemotingFuture;
import io.github.xinfra.lab.remoting.impl.server.handler.EchoRequest;
import io.github.xinfra.lab.remoting.impl.server.handler.EchoRequestHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class RemotingServerTest {

	private static RemotingServer remotingServer;

	private static RemotingClient remotingClient;

	private static RequestApi echoApi = RequestApi.of("/echo");

	private static CallOptions callOptions = new CallOptions();

	@BeforeAll
	public static void beforeAll() {
		remotingClient = new RemotingClient();
		remotingClient.startup();
        remotingClient.registerRequestHandler(echoApi, new EchoRequestHandler());

		RemotingServerConfig config = new RemotingServerConfig();
		config.setManageConnection(true);
		remotingServer = new RemotingServer(config);
		remotingServer.startup();
		remotingServer.registerRequestHandler(echoApi, new EchoRequestHandler());
	}

	@AfterAll
	public static void afterAll() {
		remotingClient.shutdown();
		remotingServer.shutdown();
	}

	@Test
	public void testSyncCall() throws RemotingException, InterruptedException {
		SocketAddress serverAddress = remotingServer.localAddress();
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);
		String result = remotingClient.syncCall(echoApi, request, serverAddress, callOptions);
		Assertions.assertEquals(result, "echo:" + msg);

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		result = remotingServer.syncCall(echoApi, request, connection.getChannel().localAddress(), callOptions);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testFutureCall() throws RemotingException, InterruptedException, TimeoutException {
		SocketAddress serverAddress = remotingServer.localAddress();
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);
		RemotingFuture<String> future = remotingClient.asyncCall(echoApi, request, serverAddress, callOptions);

		String result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		future = remotingServer.asyncCall(echoApi, request, connection.getChannel().localAddress(), callOptions);
		result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testAsyncCall() throws RemotingException, InterruptedException, TimeoutException {
		SocketAddress serverAddress = remotingServer.localAddress();
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> result = new AtomicReference<>();
		remotingClient.asyncCall(echoApi, request, serverAddress, callOptions, new RemotingCallBack<String>() {
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

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		CountDownLatch countDownLatch2 = new CountDownLatch(1);
		AtomicReference<String> result2 = new AtomicReference<>();
		remotingServer.asyncCall(echoApi, request, connection.getChannel().localAddress(), callOptions, new RemotingCallBack<String>() {
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
		SocketAddress serverAddress = remotingServer.localAddress();

		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);

		remotingClient.oneway(echoApi, request, serverAddress, callOptions);

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		remotingServer.oneway(echoApi, request, connection.getChannel().localAddress(), callOptions);

		TimeUnit.SECONDS.sleep(2);
	}

//	@Test
//	public void testRegisterUserProcessor() throws RemotingException, InterruptedException, TimeoutException {
//		testProtocol = spy(testProtocol);
//		MessageHandler messageHandler = mock(MessageHandler.class);
//		doReturn(messageHandler).when(testProtocol).messageHandler();
//
//		ServerConfig config = new ServerConfig();
//		config.setPort(findAvailableTcpPort());
//		config.setManageConnection(true);
//
//		AbstractServer server = new AbstractServer(config) {
//			@Override
//			public Protocol protocol() {
//				return testProtocol;
//			}
//		};
//
//		server.startup();
//
//		UserProcessor<String> userProcessor1 = new UserProcessor<String>() {
//			@Override
//			public String interest() {
//				return String.class.getName();
//			}
//
//			@Override
//			public Object handRequest(String request) {
//				// do nothing
//				return null;
//			}
//		};
//
//		server.registerUserProcessor(userProcessor1);
//
//		verify(messageHandler, times(1)).registerUserProcessor(eq(userProcessor1));
//	}

}
