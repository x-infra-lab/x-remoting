package io.github.xinfra.lab.remoting.connection;

import lombok.Getter;

@Getter
public class ConnectionConfig {

	private boolean idleSwitch = true;

	private long idleReaderTimeout = 15000L;

	private long idleWriterTimeout = 15000L;

	private long idleAllTimeout = 15000L;

	private int connectTimeout = 1000;

}
