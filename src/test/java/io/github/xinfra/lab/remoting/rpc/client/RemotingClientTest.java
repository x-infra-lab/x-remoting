package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.server.RemotingServer;
import io.github.xinfra.lab.remoting.rpc.handler.EchoRequest;
import io.github.xinfra.lab.remoting.rpc.handler.EchoRequestHandler;
import io.github.xinfra.lab.remoting.rpc.handler.ExceptionRequestHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.xinfra.lab.remoting.rpc.handler.RequestApis.echoPath;
import static io.github.xinfra.lab.remoting.rpc.handler.RequestApis.exceptionPath;

public class RemotingClientTest {

	private static RemotingServer remotingServer;

	private static RemotingClient remotingClient;

	private static CallOptions callOptions = CallOptions.defaults();

	@BeforeAll
	public static void beforeAll() {
		remotingServer = new RemotingServer();
		remotingServer.startup();

		remotingServer.registerRequestHandler(echoPath, new EchoRequestHandler());
		remotingServer.registerRequestHandler(exceptionPath, new ExceptionRequestHandler());
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
		String result = remotingClient.blockingCall(echoPath, request, remotingServer.getLocalAddress(), callOptions);

		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testSyncCallException() {
		String msg = "test UserProcessor throw Exception";

		RemotingException remotingException = Assertions.assertThrows(RemotingException.class, () -> {
			remotingClient.blockingCall(exceptionPath, msg, remotingServer.getLocalAddress(), callOptions);
		});

		Assertions.assertInstanceOf(IllegalArgumentException.class, remotingException.getCause());

	}

	@Test
	public void testFutureCall() throws RemotingException, InterruptedException, TimeoutException {
		String msg = "hello x-remoting";
		EchoRequest request = new EchoRequest(msg);
		RemotingFuture<String> future = remotingClient.futureCall(echoPath, request, remotingServer.getLocalAddress(),
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
		remotingClient.asyncCall(echoPath, request, remotingServer.getLocalAddress(), callOptions,
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

		remotingClient.oneway(echoPath, request, remotingServer.getLocalAddress(), callOptions);
		TimeUnit.SECONDS.sleep(2);
	}

}
