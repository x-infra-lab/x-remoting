package io.github.xinfra.lab.remoting.connection;

import lombok.Data;

@Data
public class ConnectionConfig {

    private int connectTimeout = 1000;

    private int connNum = 1;

    private boolean connWarmup = true;

}
