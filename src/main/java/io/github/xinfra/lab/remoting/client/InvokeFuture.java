package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageType;
import io.github.xinfra.lab.remoting.rpc.processor.MessageProcessor;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.util.Timeout;
import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class InvokeFuture<T extends ResponseMessage> implements Future<ResponseMessage> {

	@Getter
	private final int requestId;

	private final Protocol protocol;

	private final CountDownLatch countDownLatch;

	private volatile ResponseMessage responseMessage;

	@AccessForTest
	protected volatile Timeout timeout;

	private volatile InvokeCallBack invokeCallBack;

	private final AtomicBoolean callBackExecuted = new AtomicBoolean(false);

	private final ClassLoader classLoader;

	public InvokeFuture(int requestId, Protocol protocol) {
		this.requestId = requestId;
		this.protocol = protocol;
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

	public void asyncExecuteCallBack() {
		try {
			MessageHandler messageHandler = protocol.messageHandler();

			ExecutorService responseMessageExecutor = null;
			MessageProcessor<?> responseMessageProcessor = messageHandler.messageProcessor(RpcMessageType.response);
			if (responseMessageProcessor != null) {
				responseMessageExecutor = responseMessageProcessor.executor();
			}
			ExecutorService messageExecutor = messageHandler.executor();
			Executor executor = responseMessageExecutor != null ? responseMessageExecutor : messageExecutor;

			executor.execute(() -> {
				try {
					executeCallBack();
				}
				catch (Throwable t) {
					log.error("executeCallBack fail. id:{}", responseMessage.id(), t);
				}
			});

		}
		catch (Exception e) {
			log.error("asyncExecuteCallBack fail. id:{}", responseMessage.id(), e);
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
						invokeCallBack.complete(responseMessage);
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
