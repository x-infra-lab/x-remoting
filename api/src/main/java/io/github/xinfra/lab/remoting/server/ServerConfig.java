package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.netty.util.Timer;
import lombok.Data;

import java.util.concurrent.Executor;

@Data
public class ServerConfig {

	private String hostName;

	private int port;

	private boolean manageConnection = false;

	private boolean idleSwitch = true;

	private long idleReaderTimeout = 0L;

	private long idleWriterTimeout = 0L;

	private long idleAllTimeout = 90000L;

	private SerializationType serializationType = SerializationType.Hession;

	/**
	 * The executor is not managed by x-remoting. It must be shutdown externally.
	 */
	Executor executor;

	/**
	 * The timer is not managed by x-remoting. It must be shutdown externally.
	 */
	Timer timer;

}
