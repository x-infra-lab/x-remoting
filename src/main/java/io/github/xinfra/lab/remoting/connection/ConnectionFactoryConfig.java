package io.github.xinfra.lab.remoting.connection;

import lombok.Getter;

@Getter
public class ConnectionFactoryConfig {

    private boolean idleSwitch = true;
    private long idleReaderTimeout = 15000L;
    private long idleWriterTimeout = 15000L;
    private long idleAllTimeout = 15000L;
}
