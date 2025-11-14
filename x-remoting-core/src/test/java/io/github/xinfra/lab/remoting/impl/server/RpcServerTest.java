package io.github.xinfra.lab.remoting.impl.server;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.impl.client.RemotingClient;
import io.github.xinfra.lab.remoting.impl.client.RemotingCallBack;
import io.github.xinfra.lab.remoting.impl.client.RemotingFuture;
import io.github.xinfra.lab.remoting.impl.client.SimpleRequest;
import io.github.xinfra.lab.remoting.impl.client.SimpleUserProcessor;
import io.github.xinfra.lab.remoting.server.AbstractServer;
import io.github.xinfra.lab.remoting.server.ServerConfig;
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

	private static RemotingServer defaultRemotingServer;

	private static RemotingClient remotingClient;

	@BeforeAll
	public static void beforeAll() {
		remotingClient = new RemotingClient();
		remotingClient.startup();
		remotingClient.registerUserProcessor(new SimpleUserProcessor());

		RemotingServerConfig config = new RemotingServerConfig();
		config.setManageConnection(true);
		defaultRemotingServer = new RemotingServer(config);
		defaultRemotingServer.startup();
		defaultRemotingServer.registerUserProcessor(new SimpleUserProcessor());
	}

	@AfterAll
	public static void afterAll() {
		remotingClient.shutdown();

		defaultRemotingServer.shutdown();
	}

	@Test
	public void testSyncCall() throws RemotingException, InterruptedException {
		SocketAddress serverAddress = defaultRemotingServer.localAddress();
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);
		String result = remotingClient.syncCall(request, serverAddress, 1000);
		Assertions.assertEquals(result, "echo:" + msg);

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		result = defaultRemotingServer.syncCall(request, connection.getChannel().localAddress(), 1000);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testAsyncCall1() throws RemotingException, InterruptedException, TimeoutException {
		SocketAddress serverAddress = defaultRemotingServer.localAddress();
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);
		RemotingFuture<String> future = remotingClient.asyncCall(request, serverAddress, 1000);

		String result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		future = defaultRemotingServer.asyncCall(request, connection.getChannel().localAddress(), 1000);
		result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testAsyncCall2() throws RemotingException, InterruptedException, TimeoutException {
		SocketAddress serverAddress = defaultRemotingServer.localAddress();
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> result = new AtomicReference<>();
		remotingClient.asyncCall(request, serverAddress, 1000, new RemotingCallBack<String>() {
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
		defaultRemotingServer.asyncCall(request, connection.getChannel().localAddress(), 1000,
				new RemotingCallBack<String>() {
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
		SocketAddress serverAddress = defaultRemotingServer.localAddress();

		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);

		remotingClient.oneway(request, serverAddress);

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		defaultRemotingServer.oneway(request, connection.getChannel().localAddress());

		TimeUnit.SECONDS.sleep(2);
	}

	@Test
	public void testRegisterUserProcessor() throws RemotingException, InterruptedException, TimeoutException {
		testProtocol = spy(testProtocol);
		MessageHandler messageHandler = mock(MessageHandler.class);
		doReturn(messageHandler).when(testProtocol).messageHandler();

		ServerConfig config = new ServerConfig();
		config.setPort(findAvailableTcpPort());
		config.setManageConnection(true);

		AbstractServer server = new AbstractServer(config) {
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
