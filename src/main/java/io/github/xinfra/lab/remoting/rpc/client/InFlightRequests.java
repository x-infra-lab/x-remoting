package io.github.xinfra.lab.remoting.rpc.client;

import org.apache.commons.lang3.Validate;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.message.MessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.ResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;
import io.netty.util.AttributeKey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class InFlightRequests {

	public static final AttributeKey<InFlightRequests> KEY = AttributeKey.valueOf("rpc.inFlightRequests");

	private final ConcurrentHashMap<Integer, InvokeFuture<?>> map = new ConcurrentHashMap<>();

	public static InFlightRequests of(Connection connection) {
		return connection.getChannel().attr(KEY).get();
	}

	public static InFlightRequests getOrCreate(Connection connection) {
		io.netty.util.Attribute<InFlightRequests> attr = connection.getChannel().attr(KEY);
		InFlightRequests existing = attr.get();
		if (existing != null) {
			return existing;
		}
		InFlightRequests ifr = new InFlightRequests();
		InFlightRequests prev = attr.setIfAbsent(ifr);
		if (prev != null) {
			return prev;
		}
		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		Executor executor = connection.getExecutor();
		connection.addCloseHook(() -> ifr.cancelAll(protocol, executor));
		return ifr;
	}

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

	public void cancelAll(RpcProtocol protocol, Executor executor) {
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
