package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.protocol.Protocol;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public interface RemotingServer extends LifeCycle {

	SocketAddress localAddress();

	void registerUserProcessor(UserProcessor<?> userProcessor);

	Protocol protocol();

}
