package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponses;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RemotingFuture<T> {

	private InvokeFuture invokeFuture;

	public RemotingFuture(InvokeFuture invokeFuture) {
		this.invokeFuture = invokeFuture;
	}

	public T get() throws InterruptedException, RemotingException {
		RemotingResponseMessage responseMessage = (RemotingResponseMessage) invokeFuture.get();
		return RemotingResponses.getResponseObject(responseMessage);
	}

	public T get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, RemotingException {
		RemotingResponseMessage responseMessage = (RemotingResponseMessage) invokeFuture.get(timeout, unit);
		return RemotingResponses.getResponseObject(responseMessage);
	}

	public boolean isDone() {
		return invokeFuture.isDone();
	}

}