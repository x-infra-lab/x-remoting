package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class DefaultConnectionEventProcessor extends AbstractLifeCycle implements ConnectionEventProcessor {

	private final Thread connectionEventThread = new NamedThreadFactory("RemotingClient-Connection-Event")
		.newThread(new EventTask());

	protected LinkedBlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();

	private CopyOnWriteArrayList<ConnectionEventListener> listeners = new CopyOnWriteArrayList<>();

	@Override
	public void startup() {
		super.startup();
		connectionEventThread.start();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		connectionEventThread.interrupt();
		listeners.clear();
		eventQueue.clear();
	}

	@Override
	public void handleEvent(ConnectionEvent event, Connection connection) {
		ensureStarted();
		Validate.notNull(connection, "connection must not be null");
		eventQueue.add(new Event(event, connection));
	}

	@Override
	public void addConnectionEventListener(ConnectionEventListener listener) {
		ensureStarted();
		Validate.notNull(listener, "listener must not be null");
		listeners.addIfAbsent(listener);
	}

	@AllArgsConstructor
	@Getter
	class Event {

		ConnectionEvent connectionEvent;

		Connection connection;

	}

	class EventTask implements Runnable {

		@Override
		public void run() {
			while (isStarted()) {
				Event event;
				try {
					event = eventQueue.take();
				}
				catch (InterruptedException e) {
					continue;
				}

				for (ConnectionEventListener listener : listeners) {
					try {
						listener.onEvent(event.connectionEvent, event.connection);
					}
					catch (Throwable t) {
						log.warn("{} onEvent execute fail", listener, t);
					}
				}

			}
		}

	}

}
