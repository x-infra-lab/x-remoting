package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultReconnector extends AbstractLifeCycle implements Reconnector {

	private Set<SocketAddress> disabledAddresses = new CopyOnWriteArraySet<>();

	@AccessForTest
	protected LinkedBlockingQueue<SocketAddress> reconnectAddressQueue = new LinkedBlockingQueue<>();

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
		disabledAddresses.clear();
		reconnectAddressQueue.clear();
	}

	@Override
	public synchronized void reconnect(SocketAddress socketAddress) {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress must not be null");
		reconnectAddressQueue.add(socketAddress);
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
				SocketAddress socketAddress = null;
				try {
					socketAddress = reconnectAddressQueue.take();
				}
				catch (InterruptedException e) {
					continue;
				}

				if (disabledAddresses.contains(socketAddress)) {
					log.warn("reconnect to {} has been disabled", socketAddress);
				}
				else {
					try {
						connectionManager.connect(socketAddress);
					}
					catch (Throwable e) {
						log.warn("reconnect {} fail.", socketAddress);
						reconnectAddressQueue.add(socketAddress);
					}
				}

				try {
					TimeUnit.SECONDS.sleep(1);
				}
				catch (InterruptedException e) {
					continue;
				}

			}
		}

	}

}
