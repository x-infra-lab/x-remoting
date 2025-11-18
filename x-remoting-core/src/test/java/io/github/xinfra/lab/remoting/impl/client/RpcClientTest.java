package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.exception.RemotingServerException;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.server.RemotingServer;
import io.github.xinfra.lab.remoting.impl.server.handler.EchoRequest;
import io.github.xinfra.lab.remoting.impl.server.handler.EchoRequestHandler;
import io.github.xinfra.lab.remoting.impl.server.handler.ExceptionRequestHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class RpcClientTest {

	private static RemotingServer remotingServer;

	private static RemotingClient remotingClient;

	private static RequestApi echoApi = RequestApi.of("/echo");

	private static RequestApi exceptionApi = RequestApi.of("/exception");

	private static CallOptions callOptions = new CallOptions();

	@BeforeAll
	public static void beforeAll() {
		remotingServer = new RemotingServer();
		remotingServer.startup();

		remotingServer.registerRequestHandler(echoApi, new EchoRequestHandler());
		remotingServer.registerRequestHandler(exceptionApi, new ExceptionRequestHandler());
	}

	@AfterAll
	public static void afterAll() {
		remotingServer.shutdown();
	}

	@BeforeEach
	public void beforeEach() {
		remotingClient = new RemotingClient();
		remotingClient.startup();
	}

	@AfterEach
	public void afterEach() {
		remotingClient.shutdown();
	}

	@Test
	public void testSyncCall() throws RemotingException, InterruptedException {
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);
		String result = remotingClient.syncCall(echoApi, request, remotingServer.localAddress(), callOptions);

		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testSyncCallException() throws RemotingException, InterruptedException {
		String msg = "test UserProcessor throw Exception";

		RemotingException remotingException = Assertions.assertThrows(RemotingException.class, () -> {
			remotingClient.syncCall(exceptionApi, msg, remotingServer.localAddress(), callOptions);
		});

		Assertions.assertInstanceOf(RemotingServerException.class, remotingException.getCause());

		remotingException.printStackTrace();
	}

	@Test
	public void testFutureCall() throws RemotingException, InterruptedException, TimeoutException {
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);
		RemotingFuture<String> future = remotingClient.asyncCall(echoApi, request, remotingServer.localAddress(),
				callOptions);

		String result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testAsyncCall() throws RemotingException, InterruptedException, TimeoutException {
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> result = new AtomicReference<>();
		remotingClient.asyncCall(echoApi, request, remotingServer.localAddress(), callOptions,
				new RemotingCallBack<String>() {
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
	}

	@Test
	public void testOnewayCall() throws RemotingException, InterruptedException {
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);

		remotingClient.oneway(echoApi, request, remotingServer.localAddress(), callOptions);
		TimeUnit.SECONDS.sleep(2);
	}

}
