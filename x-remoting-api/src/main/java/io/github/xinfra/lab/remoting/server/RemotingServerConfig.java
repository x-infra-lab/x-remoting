package io.github.xinfra.lab.remoting.server;

import lombok.Data;

import java.util.concurrent.Executor;

@Data
public class RemotingServerConfig {

	private int port;

	private boolean manageConnection = false;

	private boolean idleSwitch = true;

	private long idleReaderTimeout = 0L;

	private long idleWriterTimeout = 0L;

	private long idleAllTimeout = 90000L;

	Executor executor;

}
