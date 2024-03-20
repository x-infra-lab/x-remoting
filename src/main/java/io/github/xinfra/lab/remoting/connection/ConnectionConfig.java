package io.github.xinfra.lab.remoting.connection;

import lombok.Data;

@Data
public class ConnectionConfig {

    private int connectTimeoutMills;

    private int connNum = 1;

    private boolean connWarmup;

}
