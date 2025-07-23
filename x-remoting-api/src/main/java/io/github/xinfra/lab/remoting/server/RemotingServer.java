package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.protocol.Protocol;

import java.net.SocketAddress;
import java.util.concurrent.Executor;

public interface RemotingServer extends LifeCycle {

	SocketAddress localAddress();

	Protocol protocol();

}
