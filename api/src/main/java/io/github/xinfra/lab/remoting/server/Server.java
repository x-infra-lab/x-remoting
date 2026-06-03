package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.protocol.Protocol;

import java.net.InetSocketAddress;

public interface Server extends LifeCycle {

	InetSocketAddress getLocalAddress();

	Protocol getProtocol();

}
