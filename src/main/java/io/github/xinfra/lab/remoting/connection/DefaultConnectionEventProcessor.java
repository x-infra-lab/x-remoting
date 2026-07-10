package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import org.apache.commons.lang3.Validate;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultConnectionEventProcessor extends AbstractLifeCycle implements ConnectionEventProcessor {

	private final CopyOnWriteArrayList<ConnectionEventListener> listeners = new CopyOnWriteArrayList<>();

	private final ExecutorService userDispatcher;

	private ExecutorService dispatcher;

	/**
	 * Use a dedicated single-thread dispatcher owned by this processor.
	 */
	public DefaultConnectionEventProcessor() {
		this(null);
	}

	/**
	 * Use a caller-supplied executor as the dispatcher. The caller is responsible for its
	 * lifecycle — {@link #shutdown()} will not shut it down. Pass {@code null} to fall
	 * back to the default single-thread dispatcher.
	 */
	public DefaultConnectionEventProcessor(ExecutorService userDispatcher) {
		this.userDispatcher = userDispatcher;
	}

	@Override
	public void startup() {
		super.startup();
		if (userDispatcher != null) {
			this.dispatcher = userDispatcher;
		}
		else {
			this.dispatcher = Executors.newSingleThreadExecutor(new NamedThreadFactory("Connection-Event", true));
		}
	}

	@Override
	public void shutdown() {
		super.shutdown();
		listeners.clear();
		if (userDispatcher == null && dispatcher != null) {
			dispatcher.shutdown();
			try {
				if (!dispatcher.awaitTermination(5, TimeUnit.SECONDS)) {
					dispatcher.shutdownNow();
				}
			}
			catch (InterruptedException e) {
				dispatcher.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public void handleEvent(ConnectionEvent event, Connection connection) {
		ensureStarted();
		Validate.notNull(connection, "connection must not be null");
		try {
			dispatcher.execute(() -> dispatch(event, connection));
		}
		catch (RejectedExecutionException e) {
			log.warn("event dispatch rejected for {} on {}; processor may be shutting down", event,
					connection.remoteAddress());
		}
	}

	@Override
	public void addConnectionEventListener(ConnectionEventListener listener) {
		ensureStarted();
		Validate.notNull(listener, "listener must not be null");
		listeners.addIfAbsent(listener);
	}

	private void dispatch(ConnectionEvent event, Connection connection) {
		for (ConnectionEventListener listener : listeners) {
			Executor listenerExecutor = listener.executor();
			if (listenerExecutor != null) {
				try {
					listenerExecutor.execute(() -> safeOnEvent(listener, event, connection));
				}
				catch (RejectedExecutionException e) {
					log.warn("listener executor rejected event {} for {}", event, listener);
				}
			}
			else {
				safeOnEvent(listener, event, connection);
			}
		}
	}

	private void safeOnEvent(ConnectionEventListener listener, ConnectionEvent event, Connection connection) {
		try {
			listener.onEvent(event, connection);
		}
		catch (Throwable t) {
			log.warn("{} onEvent execute fail", listener, t);
		}
	}

}
