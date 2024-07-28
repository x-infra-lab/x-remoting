package io.github.xinfra.lab.remoting.server;

import lombok.Data;

@Data
public class RemotingServerConfig {

    private int port;

    private boolean manageConnection = false;

    private boolean idleSwitch = false;

}
