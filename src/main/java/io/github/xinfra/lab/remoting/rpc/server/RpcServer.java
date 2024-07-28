package io.github.xinfra.lab.remoting.rpc.server;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeCallBack;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeFuture;
import io.github.xinfra.lab.remoting.server.BaseRemotingServer;

public class RpcServer extends BaseRemotingServer {

    private RpcServerRemoting rpcServerRemoting;

    public RpcServer(RpcServerConfig config) {
        super(config);
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

    public <R> R syncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws InterruptedException, RemotingException {
        ensureStarted();

        return rpcServerRemoting.syncCall(request, endpoint, timeoutMills);
    }

    public <R> RpcInvokeFuture<R> asyncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws RemotingException {
        ensureStarted();

        return rpcServerRemoting.asyncCall(request, endpoint, timeoutMills);
    }

    public <R> void asyncCall(Object request, Endpoint endpoint,
                              int timeoutMills,
                              RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
        ensureStarted();

        rpcServerRemoting.asyncCall(request, endpoint, timeoutMills, rpcInvokeCallBack);
    }

    public void oneway(Object request, Endpoint endpoint) throws RemotingException {
        ensureStarted();

        rpcServerRemoting.oneway(request, endpoint);
    }
}
