package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultReconnector extends AbstractLifeCycle implements Reconnector {

	private Set<SocketAddress> disableReconnectSocketAddresses = new CopyOnWriteArraySet<>();

	private ConnectionManager connectionManager;

	private ExecutorService reconnector = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(1024), new NamedThreadFactory("Reconnector-Worker"));

	public DefaultReconnector(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void startup() {
		super.startup();
		// todo start reconnect thread
	}

	@Override
	public void shutdown() {
		super.shutdown();
		// todo shutdown reconnect thread
	}

	@Override
	public synchronized void reconnect(SocketAddress socketAddress) throws RemotingException {
		ensureStarted();
		if (disableReconnectSocketAddresses.contains(socketAddress)) {
			log.warn("socketAddress:{} is disable to reconnect", socketAddress);
			throw new RemotingException("socketAddress is disable to reconnect:" + socketAddress);
		}
		connectionManager.cconnect(socketAddress);
	}

	@Override
	public synchronized void disableReconnect(SocketAddress socketAddress) {
		ensureStarted();
		disableReconnectSocketAddresses.add(socketAddress);
	}

	@Override
	public synchronized void enableReconnect(SocketAddress socketAddress) {
		ensureStarted();
		disableReconnectSocketAddresses.remove(socketAddress);
	}

	@Override
	public synchronized Future<Void> asyncReconnect(SocketAddress socketAddress) {
		ensureStarted();
		if (disableReconnectSocketAddresses.contains(socketAddress)) {
			log.warn("socketAddress:{} is disable to asyncReconnect", socketAddress);
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(
					new RemotingException("socketAddress is disable to asyncReconnect:" + socketAddress));
			return future;
		}

		Callable<Void> callable = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					reconnect(socketAddress);
				}
				catch (Exception e) {
					log.warn("reconnect socketAddress:{} fail", socketAddress, e);
					throw e;
				}
				return null;
			}
		};

		try {
			return reconnector.submit(callable);
		}
		catch (Throwable t) {
			log.warn("asyncReconnect submit failed.", t);
			throw t;
		}
	}

}
