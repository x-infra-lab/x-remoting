package io.github.xinfra.lab.remoting.connection;

import lombok.Getter;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@Getter
public class ConnectionConfig {

	private boolean idleSwitch = true;

	private long idleReaderTimeout = 15000L;

	private long idleWriterTimeout = 15000L;

	private long idleAllTimeout = 15000L;

	private int connectTimeout = 1000;

	/**
	 * The executor is not managed by x-remoting. It must be shutdown externally.
	 */
	ExecutorService executor;

}
