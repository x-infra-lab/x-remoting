package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.exception.RpcServerException;
import io.github.xinfra.lab.remoting.rpc.server.RpcServer;
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

	private static RpcServer rpcServer;

	private RpcClient rpcClient;

	@BeforeAll
	public static void beforeAll() {
		rpcServer = new RpcServer();
		rpcServer.startup();

		rpcServer.registerUserProcessor(new SimpleUserProcessor());
		rpcServer.registerUserProcessor(new ExceptionProcessor());
	}

	@AfterAll
	public static void afterAll() {
		rpcServer.shutdown();
	}

	@BeforeEach
	public void beforeEach() {
		rpcClient = new RpcClient();
		rpcClient.startup();
	}

	@AfterEach
	public void afterEach() {
		rpcClient.shutdown();
	}

	@Test
	public void testSyncCall() throws RemotingException, InterruptedException {
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);
		String result = rpcClient.syncCall(request, rpcServer.localAddress(), 1000);

		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testSyncCallException() throws RemotingException, InterruptedException {
		String msg = "test UserProcessor throw Exception";
		ExceptionRequest request = new ExceptionRequest(msg);

		RemotingException remotingException = Assertions.assertThrows(RemotingException.class, () -> {
			rpcClient.syncCall(request, rpcServer.localAddress(), 1000);
		});

		Assertions.assertInstanceOf(RpcServerException.class, remotingException.getCause());

		remotingException.printStackTrace();
	}

	@Test
	public void testAsyncCall1() throws RemotingException, InterruptedException, TimeoutException {
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);
		RpcInvokeFuture<String> future = rpcClient.asyncCall(request, rpcServer.localAddress(), 1000);

		String result = future.get(3, TimeUnit.SECONDS);
		Assertions.assertEquals(result, "echo:" + msg);
	}

	@Test
	public void testAsyncCall2() throws RemotingException, InterruptedException, TimeoutException {
		String msg = "hello x-remoting";
		SimpleRequest request = new SimpleRequest(msg);

		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<String> result = new AtomicReference<>();
		rpcClient.asyncCall(request, rpcServer.localAddress(), 1000, new RpcInvokeCallBack<String>() {
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

		rpcClient.oneway(request, rpcServer.localAddress());
		TimeUnit.SECONDS.sleep(2);
	}

}
