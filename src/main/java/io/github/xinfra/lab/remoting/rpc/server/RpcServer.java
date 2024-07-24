package io.github.xinfra.lab.remoting.rpc.server;

import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.rpc.server.RpcServerRemoting;
import io.github.xinfra.lab.remoting.server.BaseRemotingServer;

public class RpcServer extends BaseRemotingServer {

    private RpcServerRemoting rpcServerRemoting;

    public RpcServer(int port) {
        super(port);
    }

    @Override
    public void startup() {
        super.startup();
        rpcServerRemoting = new RpcServerRemoting(connectionManager);
        rpcServerRemoting.startup();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        rpcServerRemoting.shutdown();
    }

    @Override
    public ProtocolType protocolType() {
        return RpcProtocol.RPC;
    }

}
