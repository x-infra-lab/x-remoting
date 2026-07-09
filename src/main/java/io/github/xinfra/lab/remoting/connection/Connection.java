package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.Validate;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import io.netty.util.Timer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Connection {

	public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");

	@Getter
	private final Channel channel;

	@Getter
	private final Protocol protocol;

	@Getter
	private final Executor executor;

	@Getter
	private final Timer timer;

	private final List<Runnable> closeHooks = new CopyOnWriteArrayList<>();

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

	public void addCloseHook(Runnable hook) {
		Validate.notNull(hook, "close hook can not be null");
		closeHooks.add(hook);
	}

	public SocketAddress remoteAddress() {
		return channel.remoteAddress();
	}

	public ChannelFuture close() {
		if (closed.compareAndSet(false, true)) {
			for (Runnable hook : closeHooks) {
				try {
					hook.run();
				}
				catch (Throwable t) {
					log.warn("close hook error for connection to {}", remoteAddress(), t);
				}
			}
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

}
