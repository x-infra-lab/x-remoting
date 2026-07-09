package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.common.Validate;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.protocol.Protocol;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class InFlightRequests {

	private final ConcurrentHashMap<Integer, InvokeFuture<?>> map = new ConcurrentHashMap<>();

	public void add(InvokeFuture<?> invokeFuture) {
		InvokeFuture<?> prev = map.putIfAbsent(invokeFuture.getRequestId(), invokeFuture);
		Validate.isTrue(prev == null, "requestId: %s already invoked", invokeFuture.getRequestId());
	}

	public InvokeFuture<?> remove(int requestId) {
		return map.remove(requestId);
	}

	public int size() {
		return map.size();
	}

	public void cancelAll(Protocol protocol, Executor executor) {
		MessageFactory messageFactory = protocol.getMessageFactory();
		for (int requestId : map.keySet()) {
			InvokeFuture<?> invokeFuture = map.remove(requestId);
			if (invokeFuture != null) {
				invokeFuture.cancelTimeout();
				ResponseMessage responseMessage = messageFactory.createResponse(requestId,
						invokeFuture.getRequestMessage().getSerializationType(), ResponseStatus.ConnectionClosed);
				invokeFuture.complete(responseMessage);
				invokeFuture.executeCallBack(executor);
			}
		}
	}

}
