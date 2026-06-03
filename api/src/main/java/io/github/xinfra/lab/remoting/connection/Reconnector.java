package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.LifeCycle;

import java.net.InetSocketAddress;

public interface Reconnector extends LifeCycle {

	/**
	 * Mark an endpoint as unhealthy and request a reconnect. Idempotent: calling this
	 * while a reconnect is already scheduled or in flight is a no-op. Ignored when the
	 * endpoint is {@link ReconnectState#DISABLED}, {@link ReconnectState#ABANDONED}, or
	 * the reconnector is stopped.
	 */
	void onUnhealthy(InetSocketAddress address);

	/**
	 * Cancel any scheduled or in-flight reconnect for the address and reset its state to
	 * {@link ReconnectState#IDLE}. Use this when the user explicitly disconnects.
	 */
	void cancel(InetSocketAddress address);

	/**
	 * Suspend reconnect attempts for the address. Pending timers are cancelled. The
	 * endpoint stays in {@link ReconnectState#DISABLED} until {@link #enable} is called.
	 */
	void disable(InetSocketAddress address);

	/**
	 * Resume reconnect ability for the address (from DISABLED or ABANDONED back to IDLE).
	 * Does not immediately reconnect — the next {@link #onUnhealthy} call will.
	 */
	void enable(InetSocketAddress address);

	/** Current reconnect state of the address. Returns IDLE if no task is tracked. */
	ReconnectState stateOf(InetSocketAddress address);

	/** Register a listener for reconnect lifecycle events. */
	void addListener(ReconnectListener listener);

}
