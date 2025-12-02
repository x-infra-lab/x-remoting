package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.common.Validate;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.netty.util.Timeout;
import lombok.Getter;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class InvokeFuture<T extends ResponseMessage> implements Future<ResponseMessage> {

	@Getter
	private final int requestId;

	@Getter
	protected final RequestMessage requestMessage;

	private final CountDownLatch countDownLatch;

	private volatile ResponseMessage responseMessage;

	@AccessForTest
	protected volatile Timeout timeout;

	private volatile InvokeCallBack invokeCallBack;

	private final AtomicBoolean callBackExecuted = new AtomicBoolean(false);

	private final ClassLoader classLoader;

	public InvokeFuture(RequestMessage requestMessage) {
		this.requestId = requestMessage.getId();
		this.requestMessage = requestMessage;
		this.countDownLatch = new CountDownLatch(1);
		this.classLoader = Thread.currentThread().getContextClassLoader();
	}

	public void addTimeout(Timeout timeout) {
		Validate.isTrue(this.timeout == null, "repeat add timeout for InvokeFuture");
		this.timeout = timeout;
	}

	public void addCallBack(InvokeCallBack invokeCallBack) {
		Validate.isTrue(this.invokeCallBack == null, "repeat add invokeCallBack for InvokeFuture");
		this.invokeCallBack = invokeCallBack;
	}

	public void asyncExecuteCallBack(Executor executor) {
		try {
			executor.execute(() -> {
				try {
					executeCallBack();
				}
				catch (Throwable t) {
					log.error("executeCallBack fail. getId:{}", responseMessage.getId(), t);
				}
			});
		}
		catch (Exception e) {
			log.error("asyncExecuteCallBack fail. getId:{}", responseMessage.getId(), e);
		}
	}

	public void executeCallBack() {
		if (invokeCallBack != null) {
			if (isDone()) {
				if (callBackExecuted.compareAndSet(false, true)) {
					ClassLoader contextClassLoader = null;
					try {
						ClassLoader appClassLoader = getAppClassLoader();
						if (appClassLoader != null) {
							contextClassLoader = Thread.currentThread().getContextClassLoader();
							Thread.currentThread().setContextClassLoader(appClassLoader);
						}
						invokeCallBack.onMessage(responseMessage);
					}
					finally {
						if (contextClassLoader != null) {
							Thread.currentThread().setContextClassLoader(contextClassLoader);
						}
					}
				}
			}
		}
	}

	public ClassLoader getAppClassLoader() {
		return classLoader;
	}

	public void complete(ResponseMessage responseMessage) {
		Validate.isTrue(this.responseMessage == null, "requestId: %s InvokeFuture already finished.", requestId);
		this.responseMessage = responseMessage;
		countDownLatch.countDown();
	}

	public boolean cancelTimeout() {
		if (timeout != null) {
			return timeout.cancel();
		}
		return false;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException("InvokeFuture not support method cannel");
	}

	@Override
	public boolean isCancelled() {
		throw new UnsupportedOperationException("InvokeFuture not support method isCancelled");
	}

	public boolean isDone() {
		return countDownLatch.getCount() <= 0;
	}

	@Override
	public ResponseMessage get() throws InterruptedException {
		countDownLatch.await();
		return responseMessage;
	}

	@Override
	public ResponseMessage get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
		boolean finished = countDownLatch.await(timeout, unit);
		if (!finished) {
			throw new TimeoutException("InvokeFuture timeout");
		}
		return responseMessage;
	}

}
