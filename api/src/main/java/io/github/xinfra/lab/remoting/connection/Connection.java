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
import java.util.concurrent.atomic.AtomicInteger;

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

	/**
	 * Heartbeat failure counter. Mutated concurrently by heartbeat callbacks running on
	 * the connection's executor; reads happen from the NIO thread on each idle event.
	 * Backed by {@link AtomicInteger} so updates are race-free.
	 */
	private final AtomicInteger heartbeatFailCnt = new AtomicInteger(0);

	@Getter
	@Setter
	private int heartbeatTimeoutMills = 3000;

	@Getter
	@Setter
	private int heartbeatMaxFailCount = 3;

	public int getHeartbeatFailCnt() {
		return heartbeatFailCnt.get();
	}

	/** Atomically increment the heartbeat failure count and return the new value. */
	public int incrementHeartbeatFailCnt() {
		return heartbeatFailCnt.incrementAndGet();
	}

	/** Reset the heartbeat failure count to zero (after a successful heartbeat). */
	public void resetHeartbeatFailCnt() {
		heartbeatFailCnt.set(0);
	}

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
		InvokeFuture<?> prevFuture = invokeMap.putIfAbsent(invokeFuture.getRequestId(), invokeFuture);
		Validate.isTrue(prevFuture == null, "requestId: %s already invoked", invokeFuture.getRequestId());
	}

	public InvokeFuture<?> removeInvokeFuture(Integer requestId) {
		return invokeMap.remove(requestId);
	}

	/**
	 * Returns the channel's remote address. For all production (TCP) usage this is an
	 * {@link java.net.InetSocketAddress}; the framework's public
	 * {@code ConnectionManager} / {@code Reconnector} APIs assume this and cast at the
	 * boundary.
	 */
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
				invokeFuture.executeCallBack(getExecutor());
			}
		}
	}

}
