package io.github.xinfra.lab.remoting.connection;

import java.util.concurrent.Executor;

public interface ConnectionEventListener {

	void onEvent(ConnectionEvent connectionEvent, Connection connection);

	/**
	 * Optional executor on which {@link #onEvent} will be invoked. Returning {@code null}
	 * (default) makes the {@link ConnectionEventProcessor}'s dispatcher thread run the
	 * callback inline — which means a slow listener will block dispatching to subsequent
	 * listeners and subsequent events. Provide an executor for any listener that does I/O
	 * or other potentially blocking work.
	 */
	default Executor executor() {
		return null;
	}

}
