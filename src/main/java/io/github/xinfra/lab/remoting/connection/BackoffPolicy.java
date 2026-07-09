package io.github.xinfra.lab.remoting.connection;

public interface BackoffPolicy {

	/**
	 * Compute the delay before the next reconnect attempt.
	 * @param attempts number of failed attempts so far (0 for the first try after going
	 * unhealthy)
	 * @return delay in nanoseconds, or a negative value to signal "give up"
	 */
	long nextDelayNanos(int attempts);

}
