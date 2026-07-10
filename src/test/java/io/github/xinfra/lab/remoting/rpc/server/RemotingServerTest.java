package io.github.xinfra.lab.remoting.rpc.server;

import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.client.RemotingClient;
import io.github.xinfra.lab.remoting.rpc.client.RemotingCallBack;
import io.github.xinfra.lab.remoting.rpc.client.RemotingFuture;
import io.github.xinfra.lab.remoting.rpc.handler.EchoRequest;
import io.github.xinfra.lab.remoting.rpc.handler.EchoRequestHandler;
import io.github.xinfra.lab.remoting.server.ServerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.xinfra.lab.remoting.rpc.handler.RequestApis.echoPath;

public class RemotingServerTest {

	private static RemotingServer remotingServer;

	private static RemotingClient remotingClient;

	private static CallOptions callOptions = CallOptions.defaults();

	@BeforeAll
	public static void beforeAll() {
		remotingClient = new RemotingClient();
		remotingClient.startup();
		remotingClient.registerRequestHandler(echoPath, new EchoRequestHandler());

		ServerConfig config = ServerConfig.builder().manageConnection(true).build();
		remotingServer = new RemotingServer(config);
		remotingServer.startup();
		remotingServer.registerRequestHandler(echoPath, new EchoRequestHandler());
	}

	@AfterAll
	public static void afterAll() {
		remotingClient.shutdown();
		remotingServer.shutdown();
	}

	private static InetSocketAddress clientLocalAddress(Connection connection) {
		return (InetSocketAddress) connection.getChannel().localAddress();
	}

	@Test
	public void testSyncCall() throws RemotingException, InterruptedException {
		InetSocketAddress serverAddress = remotingServer.getLocalAddress();
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);
		String result = remotingClient.blockingCall(echoPath, request, serverAddress, callOptions);
		Assertions.assertEquals(result, "echo:" + msg);

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		result = remotingServer.blockingCall(echoPath, request, clientLocalAddress(connection), callOptions);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testFutureCall() throws RemotingException, InterruptedException, TimeoutException {
		InetSocketAddress serverAddress = remotingServer.getLocalAddress();
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);
		RemotingFuture<String> future = remotingClient.futureCall(echoPath, request, serverAddress, callOptions);

		String result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		future = remotingServer.futureCall(echoPath, request, clientLocalAddress(connection), callOptions);
		result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testAsyncCall() throws RemotingException, InterruptedException, TimeoutException {
		InetSocketAddress serverAddress = remotingServer.getLocalAddress();
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> result = new AtomicReference<>();
		remotingClient.asyncCall(echoPath, request, serverAddress, callOptions, new RemotingCallBack<String>() {
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
		remotingServer.asyncCall(echoPath, request, clientLocalAddress(connection), callOptions,
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
		InetSocketAddress serverAddress = remotingServer.getLocalAddress();

		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);

		remotingClient.oneway(echoPath, request, serverAddress, callOptions);

		Connection connection = remotingClient.getConnectionManager().get(serverAddress);
		remotingServer.oneway(echoPath, request, clientLocalAddress(connection), callOptions);

		TimeUnit.SECONDS.sleep(2);
	}

}
