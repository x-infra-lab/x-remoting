package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import org.apache.commons.lang3.Validate;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultReconnector extends AbstractLifeCycle implements Reconnector {

	protected volatile ConnectionManager connectionManager;

	private final ReconnectConfig config;

	protected final ConcurrentHashMap<InetSocketAddress, EndpointReconnectTask> tasks = new ConcurrentHashMap<>();

	private final CopyOnWriteArrayList<ReconnectListener> listeners = new CopyOnWriteArrayList<>();

	private Timer timer;

	private ExecutorService executor;

	public DefaultReconnector(ConnectionManager connectionManager) {
		this(connectionManager, ReconnectConfig.defaults());
	}

	public DefaultReconnector(ConnectionManager connectionManager, ReconnectConfig config) {
		Validate.notNull(connectionManager, "connectionManager must not be null");
		Validate.notNull(config, "config must not be null");
		this.connectionManager = connectionManager;
		this.config = config;
	}

	@Override
	public void startup() {
		super.startup();
		this.timer = new HashedWheelTimer(new NamedThreadFactory("RemotingClient-Reconnect-Timer", true));
		this.executor = Executors.newFixedThreadPool(Math.max(1, config.getWorkerThreads()),
				new NamedThreadFactory("RemotingClient-Reconnect-Worker", true));
	}

	@Override
	public void shutdown() {
		super.shutdown();
		for (EndpointReconnectTask task : tasks.values()) {
			task.stop();
		}
		tasks.clear();
		if (executor != null) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			}
			catch (InterruptedException e) {
				executor.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}
		if (timer != null) {
			timer.stop();
		}
		listeners.clear();
	}

	@Override
	public void onUnhealthy(InetSocketAddress address) {
		ensureStarted();
		Validate.notNull(address, "address must not be null");
		taskOf(address).onUnhealthy();
	}

	@Override
	public void cancel(InetSocketAddress address) {
		Validate.notNull(address, "address must not be null");
		EndpointReconnectTask task = tasks.get(address);
		if (task != null) {
			task.cancel();
		}
	}

	@Override
	public void disable(InetSocketAddress address) {
		ensureStarted();
		Validate.notNull(address, "address must not be null");
		taskOf(address).disable();
	}

	@Override
	public void enable(InetSocketAddress address) {
		ensureStarted();
		Validate.notNull(address, "address must not be null");
		EndpointReconnectTask task = tasks.get(address);
		if (task != null) {
			task.enable();
		}
	}

	@Override
	public ReconnectState stateOf(InetSocketAddress address) {
		Validate.notNull(address, "address must not be null");
		EndpointReconnectTask task = tasks.get(address);
		return task == null ? ReconnectState.IDLE : task.state();
	}

	@Override
	public void addListener(ReconnectListener listener) {
		Validate.notNull(listener, "listener must not be null");
		listeners.add(listener);
	}

	private EndpointReconnectTask taskOf(InetSocketAddress address) {
		return tasks.computeIfAbsent(address, EndpointReconnectTask::new);
	}

	private void fireScheduled(InetSocketAddress address, int attempts, long delayNanos) {
		for (ReconnectListener l : listeners) {
			try {
				l.onScheduled(address, attempts, delayNanos);
			}
			catch (Throwable t) {
				log.warn("ReconnectListener.onScheduled threw", t);
			}
		}
	}

	private void fireSuccess(InetSocketAddress address, int attempts) {
		for (ReconnectListener l : listeners) {
			try {
				l.onSuccess(address, attempts);
			}
			catch (Throwable t) {
				log.warn("ReconnectListener.onSuccess threw", t);
			}
		}
	}

	private void fireFailure(InetSocketAddress address, int attempts, Throwable cause) {
		for (ReconnectListener l : listeners) {
			try {
				l.onFailure(address, attempts, cause);
			}
			catch (Throwable t) {
				log.warn("ReconnectListener.onFailure threw", t);
			}
		}
	}

	private void fireAbandoned(InetSocketAddress address, int attempts, Throwable lastCause) {
		for (ReconnectListener l : listeners) {
			try {
				l.onAbandoned(address, attempts, lastCause);
			}
			catch (Throwable t) {
				log.warn("ReconnectListener.onAbandoned threw", t);
			}
		}
	}

	/**
	 * Per-endpoint reconnect state machine.
	 * <p>
	 * All state transitions happen under {@code lock}. Listener callbacks are invoked
	 * <strong>outside</strong> the lock so user code cannot deadlock with reconnector
	 * calls.
	 */
	class EndpointReconnectTask {

		private final InetSocketAddress address;

		private final Object lock = new Object();

		private ReconnectState state = ReconnectState.IDLE;

		private int attempts = 0;

		private long firstFailureNanos = 0L;

		private Timeout pendingTimeout;

		private Throwable lastCause;

		EndpointReconnectTask(InetSocketAddress address) {
			this.address = address;
		}

		ReconnectState state() {
			synchronized (lock) {
				return state;
			}
		}

		void onUnhealthy() {
			boolean fireSched = false;
			boolean fireAbn = false;
			int firedAttempts = 0;
			long firedDelay = 0L;
			Throwable firedCause = null;

			synchronized (lock) {
				if (state != ReconnectState.IDLE) {
					return;
				}
				attempts = 0;
				firstFailureNanos = 0;
				lastCause = null;

				long delay = config.getBackoffPolicy().nextDelayNanos(attempts);
				if (delay < 0) {
					state = ReconnectState.ABANDONED;
					fireAbn = true;
				}
				else if (schedule(delay)) {
					fireSched = true;
					firedAttempts = attempts;
					firedDelay = delay;
				}
			}

			if (fireSched) {
				fireScheduled(address, firedAttempts, firedDelay);
			}
			if (fireAbn) {
				fireAbandoned(address, firedAttempts, firedCause);
			}
		}

		void cancel() {
			synchronized (lock) {
				cancelTimeoutLocked();
				attempts = 0;
				firstFailureNanos = 0;
				lastCause = null;
				if (state != ReconnectState.STOPPED) {
					state = ReconnectState.IDLE;
				}
			}
		}

		void disable() {
			synchronized (lock) {
				cancelTimeoutLocked();
				if (state != ReconnectState.STOPPED) {
					state = ReconnectState.DISABLED;
				}
			}
		}

		void enable() {
			synchronized (lock) {
				if (state == ReconnectState.DISABLED || state == ReconnectState.ABANDONED) {
					state = ReconnectState.IDLE;
					attempts = 0;
					firstFailureNanos = 0;
					lastCause = null;
				}
			}
		}

		void stop() {
			synchronized (lock) {
				cancelTimeoutLocked();
				state = ReconnectState.STOPPED;
			}
		}

		private void cancelTimeoutLocked() {
			if (pendingTimeout != null) {
				pendingTimeout.cancel();
				pendingTimeout = null;
			}
		}

		private boolean exhaustedLocked() {
			if (config.getMaxAttempts() > 0 && attempts >= config.getMaxAttempts()) {
				return true;
			}
			if (config.getMaxTotalDurationNanos() > 0 && firstFailureNanos > 0
					&& (System.nanoTime() - firstFailureNanos) >= config.getMaxTotalDurationNanos()) {
				return true;
			}
			return false;
		}

		/** Caller must hold {@code lock}. Returns true if the timer was scheduled. */
		private boolean schedule(long delayNanos) {
			try {
				pendingTimeout = timer.newTimeout(t -> {
					try {
						executor.execute(this::runAttempt);
					}
					catch (RejectedExecutionException e) {
						log.debug("reconnect executor rejected for {}", address);
					}
				}, delayNanos, TimeUnit.NANOSECONDS);
				state = ReconnectState.SCHEDULED;
				return true;
			}
			catch (Throwable t) {
				log.warn("schedule reconnect for {} failed", address, t);
				state = ReconnectState.STOPPED;
				return false;
			}
		}

		private void runAttempt() {
			synchronized (lock) {
				if (state != ReconnectState.SCHEDULED) {
					return;
				}
				pendingTimeout = null;
				state = ReconnectState.CONNECTING;
			}

			Throwable failure = null;
			try {
				connectionManager.connect(address);
			}
			catch (Throwable e) {
				failure = e;
			}

			boolean fireOk = false;
			boolean fireFail = false;
			boolean fireSched = false;
			boolean fireAbn = false;
			int firedAttempts = 0;
			long firedDelay = 0L;
			Throwable firedCause = null;

			synchronized (lock) {
				if (state != ReconnectState.CONNECTING) {
					return;
				}
				if (failure == null) {
					firedAttempts = attempts + 1;
					state = ReconnectState.IDLE;
					attempts = 0;
					firstFailureNanos = 0;
					lastCause = null;
					fireOk = true;
				}
				else {
					attempts++;
					if (firstFailureNanos == 0) {
						firstFailureNanos = System.nanoTime();
					}
					lastCause = failure;
					fireFail = true;
					firedAttempts = attempts;
					firedCause = failure;

					long delay = config.getBackoffPolicy().nextDelayNanos(attempts);
					if (delay < 0 || exhaustedLocked()) {
						state = ReconnectState.ABANDONED;
						fireAbn = true;
					}
					else if (schedule(delay)) {
						fireSched = true;
						firedDelay = delay;
					}
				}
			}

			if (fireFail) {
				fireFailure(address, firedAttempts, firedCause);
			}
			if (fireOk) {
				fireSuccess(address, firedAttempts);
			}
			if (fireSched) {
				fireScheduled(address, firedAttempts, firedDelay);
			}
			if (fireAbn) {
				fireAbandoned(address, firedAttempts, firedCause);
			}
		}

	}

}
