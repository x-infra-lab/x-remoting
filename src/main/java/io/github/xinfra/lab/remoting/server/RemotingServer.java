package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;

import java.net.InetSocketAddress;

public interface RemotingServer extends LifeCycle {
    InetSocketAddress localAddress();

    void registerUserProcessor(UserProcessor<?> userProcessor);

    ProtocolType protocolType();
}
