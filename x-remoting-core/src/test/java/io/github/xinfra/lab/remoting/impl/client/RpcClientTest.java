package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.exception.RemotingServerException;
import io.github.xinfra.lab.remoting.impl.server.RemotingServer;
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

public class RpcClientTest {

	private static RemotingServer defaultRemotingServer;

	private RemotingClient remotingClient;

	@BeforeAll
	public static void beforeAll() {
		defaultRemotingServer = new RemotingServer();
		defaultRemotingServer.startup();

		defaultRemotingServer.registerUserProcessor(new SimpleUserProcessor());
		defaultRemotingServer.registerUserProcessor(new ExceptionProcessor());
	}

	@AfterAll
	public static void afterAll() {
		defaultRemotingServer.shutdown();
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
		SimpleRequest request = new SimpleRequest(msg);
		String result = remotingClient.syncCall(request, defaultRemotingServer.localAddress(), 1000);

		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testSyncCallException() throws RemotingException, InterruptedException {
		String msg = "test UserProcessor throw Exception";
		ExceptionRequest request = new ExceptionRequest(msg);

		RemotingException remotingException = Assertions.assertThrows(RemotingException.class, () -> {
			remotingClient.syncCall(request, defaultRemotingServer.localAddress(), 1000);
		});

		Assertions.assertInstanceOf(RemotingServerException.class, remotingException.getCause());

		remotingException.printStackTrace();
	}

	@Test
	public void testAsyncCall1() throws RemotingException, InterruptedException, TimeoutException {
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);
		RemotingFuture<String> future = remotingClient.asyncCall(request, defaultRemotingServer.localAddress(), 1000);

		String result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testAsyncCall2() throws RemotingException, InterruptedException, TimeoutException {
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> result = new AtomicReference<>();
		remotingClient.asyncCall(request, defaultRemotingServer.localAddress(), 1000, new RemotingCallBack<String>() {
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
		SimpleRequest request = new SimpleRequest(msg);

		remotingClient.oneway(request, defaultRemotingServer.localAddress());
		TimeUnit.SECONDS.sleep(2);
	}

}
