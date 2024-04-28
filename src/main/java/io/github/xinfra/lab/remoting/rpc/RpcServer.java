package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.server.BaseRemotingServer;

public class RpcServer extends BaseRemotingServer {

    private  RpcServerRemoting rpcServerRemoting;

    public RpcServer(int port) {
        super(port);
    }

    @Override
    public void startup() {
        super.startup();
        rpcServerRemoting = new RpcServerRemoting(connectionManager);
    }

    @Override
    public ProtocolType protocolType() {
        return ProtocolType.RPC;
    }


}
