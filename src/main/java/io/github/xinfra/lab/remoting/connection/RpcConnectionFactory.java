package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.protocol.ProtocolType;

public class RpcConnectionFactory extends DefaultConnectionFactory{
    public RpcConnectionFactory() {
        super(ProtocolType.RPC, new RpcHandler());
    }
}
