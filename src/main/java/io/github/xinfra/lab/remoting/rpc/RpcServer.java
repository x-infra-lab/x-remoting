package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.RpcProtocol;
import io.github.xinfra.lab.remoting.server.BaseRemotingServer;

public class RpcServer extends BaseRemotingServer {
    static {
        ProtocolManager.registerProtocolIfAbsent(ProtocolType.RPC, new RpcProtocol());
    }

    public RpcServer(int port) {
        super(port);
    }

    @Override
    public ProtocolType protocolType() {
        return ProtocolType.RPC;
    }
}
