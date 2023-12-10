package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.client.InvokeCallBack;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.connection.DefaultConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.util.concurrent.Future;

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
        // TODO
    }

    public <R> R syncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws RemotingException, InterruptedException {

        return rpcRemoting.syncCall(request, endpoint, timeoutMills);
    }

    public <R> Future<R> asyncCall(Object request, Endpoint endpoint) {
        // todo
        return null;
    }

    public <R> void asyncCall(Object request, Endpoint endpoint, InvokeCallBack invokeCallBack) {
        // todo
    }

    public void oneway(Object request, Endpoint endpoint) {
        // todo
    }

}
