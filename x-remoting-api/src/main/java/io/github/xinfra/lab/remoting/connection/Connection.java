package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.common.Validate;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import io.netty.util.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Connection {

	public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");

	@AccessForTest
	protected ConcurrentHashMap<Integer, InvokeFuture<?>> invokeMap = new ConcurrentHashMap<>();

	@Getter
	private final Channel channel;

	@Getter
	private final Protocol protocol;

	@Getter
	private final Executor executor;

	@Getter
	private final Timer timer;

	@Getter
	@Setter
	private int heartbeatFailCnt = 0;

	@Getter
	@Setter
	private int heartbeatTimeoutMills = 3000;

	@Getter
	@Setter
	private int heartbeatMaxFailCount = 3;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	public Connection(Protocol protocol, Channel channel, Executor executor, Timer timer) {
		Validate.notNull(protocol, "getProtocol can not be null");
		Validate.notNull(channel, "channel can not be null");
		Validate.notNull(executor, "executor can not be null");
		Validate.notNull(timer, "timer can not be null");
		this.protocol = protocol;
		this.channel = channel;
		this.executor = executor;
		this.timer = timer;
		this.channel.attr(CONNECTION).set(this);
		this.channel.pipeline().fireUserEventTriggered(ConnectionEvent.CONNECT);
	}

	public void addInvokeFuture(InvokeFuture<?> invokeFuture) {
		InvokeFuture<?> prevFuture = invokeMap.put(invokeFuture.getRequestId(), invokeFuture);
		Validate.isTrue(prevFuture == null, "requestId: %s already invoked", invokeFuture.getRequestId());
	}

	public InvokeFuture<?> removeInvokeFuture(Integer requestId) {
		return invokeMap.remove(requestId);
	}

	public SocketAddress remoteAddress() {
		return channel.remoteAddress();
	}

	public ChannelFuture close() {
		if (closed.compareAndSet(false, true)) {
			onClose();
			return channel.close().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						log.info("close connection to remote address:{} success", remoteAddress());
					}
					else {
						log.warn("close connection to remote address:{} fail", remoteAddress(), future.cause());
					}
				}
			});
		}
		return channel.newSucceededFuture();
	}

	public boolean isClosed() {
		return closed.get();
	}

	public void onClose() {
		for (int requestId : invokeMap.keySet()) {
			InvokeFuture<?> invokeFuture = removeInvokeFuture(requestId);
			if (invokeFuture != null) {
				invokeFuture.cancelTimeout();
				ResponseMessage responseMessage = protocol.getMessageFactory()
					.createResponse(requestId, invokeFuture.getRequestMessage().getSerializationType(),
							ResponseStatus.ConnectionClosed);
				invokeFuture.complete(responseMessage);
				invokeFuture.asyncExecuteCallBack(getExecutor());
			}
		}
	}

}
