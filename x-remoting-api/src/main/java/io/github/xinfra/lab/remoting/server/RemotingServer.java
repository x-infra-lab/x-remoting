package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.protocol.Protocol;

import java.net.SocketAddress;

public interface RemotingServer extends LifeCycle {

	SocketAddress localAddress();

	Protocol protocol();

}
