package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class InvokeFutureTest {

	private InvokeFuture<?> invokeFuture;

	private TestProtocol testProtocol;

	@BeforeEach
	public void before() {
		testProtocol = new TestProtocol();
		final int requestId = IDGenerator.nextRequestId();
		invokeFuture = new InvokeFuture<>(requestId);
	}

	@Test
	public void testTimeout() {
		Assertions.assertNull(invokeFuture.timeout);
		Assertions.assertFalse(invokeFuture.cancelTimeout());

		HashedWheelTimer timer = new HashedWheelTimer();

		Timeout timeout = timer.newTimeout(t -> {
		}, 3, TimeUnit.SECONDS);
		invokeFuture.addTimeout(timeout);

		Assertions.assertEquals(invokeFuture.timeout, timeout);
		Assertions.assertTrue(invokeFuture.cancelTimeout());
		Assertions.assertFalse(invokeFuture.cancelTimeout());
		Assertions.assertTrue(invokeFuture.timeout.isCancelled());

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			invokeFuture.addTimeout(timeout);
		});

		timer.stop();
	}

	@Test
	public void testGet() throws InterruptedException, TimeoutException {
		ResponseMessage responseMessage = mock(ResponseMessage.class);
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		executorService.submit(() -> {
			try {
				TimeUnit.SECONDS.sleep(2);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			invokeFuture.complete(responseMessage);
		});

		Assertions.assertThrows(TimeoutException.class, () -> {
			invokeFuture.get(200, TimeUnit.MILLISECONDS);
		});
		Assertions.assertFalse(invokeFuture.isDone());

		Message result = invokeFuture.get(3, TimeUnit.SECONDS);
		Assertions.assertSame(result, responseMessage);
		Assertions.assertTrue(invokeFuture.isDone());

		result = invokeFuture.get(3, TimeUnit.SECONDS);
		Assertions.assertSame(result, responseMessage);
		Assertions.assertTrue(invokeFuture.isDone());

		executorService.shutdownNow();
	}

	@Test
	public void testAppClassLoader() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Assertions.assertSame(contextClassLoader, invokeFuture.getAppClassLoader());

		try {
			URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {}, contextClassLoader);
			Thread.currentThread().setContextClassLoader(urlClassLoader);

			InvokeFuture<?> future = new InvokeFuture<>(IDGenerator.nextRequestId());
			Assertions.assertSame(future.getAppClassLoader(), urlClassLoader);
		}
		finally {
			// recover current thread context classLoader
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}

	}

	@Test
	public void testCallBackSync() {
		AtomicBoolean callbackExecuted = new AtomicBoolean(false);
		AtomicInteger callBackExecuteTimes = new AtomicInteger(0);
		InvokeCallBack callBack = message -> {
			callbackExecuted.set(true);
			callBackExecuteTimes.getAndIncrement();
		};
		invokeFuture.addCallBack(callBack);

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			invokeFuture.addCallBack(callBack);
		});

		ResponseMessage responseMessage = mock(ResponseMessage.class);
		invokeFuture.complete(responseMessage);
		invokeFuture.executeCallBack();
		Assertions.assertTrue(callbackExecuted.get());
		Assertions.assertEquals(1, callBackExecuteTimes.get());

		invokeFuture.executeCallBack();
		Assertions.assertTrue(callbackExecuted.get());
		Assertions.assertEquals(1, callBackExecuteTimes.get());
	}

	@Test
	public void testCallBackAsync() throws InterruptedException, TimeoutException {
		invokeFuture = spy(invokeFuture);

		ExecutorService executorService = Executors.newCachedThreadPool();

		AtomicBoolean callbackExecuted = new AtomicBoolean(false);
		AtomicInteger callBackExecuteTimes = new AtomicInteger(0);
		InvokeCallBack callBack = new InvokeCallBack() {
			@Override
			public void complete(ResponseMessage message) {
				callbackExecuted.set(true);
				callBackExecuteTimes.getAndIncrement();
			}
		};

		callBack = spy(callBack);
		invokeFuture.addCallBack(callBack);

		InvokeCallBack tempCallBack = callBack;
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			invokeFuture.addCallBack(tempCallBack);
		});

		ResponseMessage responseMessage = mock(ResponseMessage.class);
		invokeFuture.complete(responseMessage);
		invokeFuture.asyncExecuteCallBack(executorService);

		InvokeCallBack finalCallBack = callBack;
		Wait.untilIsTrue(() -> {
			try {
				verify(invokeFuture, atLeastOnce()).executeCallBack();
				verify(finalCallBack, atLeastOnce()).complete(eq(responseMessage));
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		Assertions.assertTrue(callbackExecuted.get());
		Assertions.assertEquals(1, callBackExecuteTimes.get());
		verify(finalCallBack, times(1)).complete(eq(responseMessage));

		invokeFuture.asyncExecuteCallBack(executorService);
		Wait.untilIsTrue(() -> {
			try {
				verify(invokeFuture, times(2)).executeCallBack();
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(invokeFuture, times(2)).executeCallBack();
		Assertions.assertEquals(1, callBackExecuteTimes.get());

		executorService.shutdownNow();
	}

}
