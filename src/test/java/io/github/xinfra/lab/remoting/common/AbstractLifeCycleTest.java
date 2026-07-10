package io.github.xinfra.lab.remoting.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractLifeCycleTest {

	@Test
	public void testShutdownIdempotent() {
		AbstractLifeCycle lifeCycle = new AbstractLifeCycle() {
		};
		lifeCycle.startup();
		Assertions.assertTrue(lifeCycle.isStarted());

		lifeCycle.shutdown();
		Assertions.assertFalse(lifeCycle.isStarted());

		lifeCycle.shutdown();
		Assertions.assertFalse(lifeCycle.isStarted());
	}

	@Test
	public void testStartupTwiceThrows() {
		AbstractLifeCycle lifeCycle = new AbstractLifeCycle() {
		};
		lifeCycle.startup();
		Assertions.assertThrows(IllegalStateException.class, lifeCycle::startup);
	}

}
