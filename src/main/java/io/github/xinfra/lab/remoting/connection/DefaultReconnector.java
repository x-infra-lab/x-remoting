package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultReconnector extends AbstractLifeCycle implements Reconnector {

	private static final long DEFAULT_RECONNECT_INTERVAL = 1000L;

	private Set<SocketAddress> disabledAddresses = new CopyOnWriteArraySet<>();

	@AccessForTest
	protected CopyOnWriteArrayList<SocketAddress> reconnectAddresses = new CopyOnWriteArrayList<>();

	@AccessForTest
	protected ConnectionManager connectionManager;

	private final Thread reconeectThread = new NamedThreadFactory("reconnect-thread").newThread(new ReconnectTask());

	public DefaultReconnector(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	@Override
	public void startup() {
		super.startup();
		reconeectThread.start();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		reconeectThread.interrupt();
		reconnectAddresses.clear();
		disabledAddresses.clear();
	}

	@Override
	public synchronized void reconnect(SocketAddress socketAddress) {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress must not be null");
		reconnectAddresses.addIfAbsent(socketAddress);
	}

	@Override
	public synchronized void disconnect(SocketAddress socketAddress) {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress must not be null");
		reconnectAddresses.remove(socketAddress);
	}

	@Override
	public synchronized void disableReconnect(SocketAddress socketAddress) {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress must not be null");
		disabledAddresses.add(socketAddress);
	}

	@Override
	public synchronized void enableReconnect(SocketAddress socketAddress) {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress must not be null");
		disabledAddresses.remove(socketAddress);
	}

	class ReconnectTask implements Runnable {

		@Override
		public void run() {
			while (isStarted()) {

				synchronized (this) {
					if (!reconnectAddresses.isEmpty()) {
						SocketAddress socketAddress = reconnectAddresses.remove(0);
						if (disabledAddresses.contains(socketAddress)) {
							log.warn("reconnect to {} has been disabled", socketAddress);
						}
						else {
							try {
								connectionManager.connect(socketAddress);
							}
							catch (Throwable e) {
								log.warn("reconnect {} fail.", socketAddress);
								reconnectAddresses.addIfAbsent(socketAddress);
							}
						}
					}
				}

				try {
					TimeUnit.MILLISECONDS.sleep(DEFAULT_RECONNECT_INTERVAL);
				}
				catch (InterruptedException e) {
					continue;
				}

			}
		}

	}

}
