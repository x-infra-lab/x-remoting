package io.github.xinfra.lab.remoting.connection;

public enum ReconnectState {

	/** No reconnect activity. */
	IDLE,

	/** A timer is waiting to fire the next attempt. */
	SCHEDULED,

	/** A connect attempt is currently running. */
	CONNECTING,

	/** Reconnect explicitly disabled by user; pending timers cancelled. */
	DISABLED,

	/** Max attempts / max duration exhausted; user must enable to retry. */
	ABANDONED,

	/** Reconnector has been shut down. */
	STOPPED

}
