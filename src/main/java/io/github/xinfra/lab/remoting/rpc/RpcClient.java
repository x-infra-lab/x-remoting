package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.connection.DefaultConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;


public class RpcClient extends AbstractLifeCycle {

    private RpcRemoting rpcRemoting;
    private ConnectionManager connectionManager;

    @Override
    public void startup() {
        super.startup();
        connectionManager = new DefaultConnectionManager();
        rpcRemoting = new RpcRemoting(connectionManager);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    public <R> R syncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws RemotingException, InterruptedException {

        return rpcRemoting.syncCall(request, endpoint, timeoutMills);
    }

    public <R> RpcInvokeFuture<R> asyncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws RemotingException {
        return rpcRemoting.asyncCall(request, endpoint, timeoutMills);
    }

    public <R> void asyncCall(Object request, Endpoint endpoint, int timeoutMills,
                              RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
        rpcRemoting.asyncCall(request, endpoint, timeoutMills, rpcInvokeCallBack);
    }

    public void oneway(Object request, Endpoint endpoint) throws RemotingException {
        rpcRemoting.oneway(request, endpoint);
    }

}
