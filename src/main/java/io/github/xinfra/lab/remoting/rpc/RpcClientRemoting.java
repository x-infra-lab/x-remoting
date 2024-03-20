package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.RemotingException;

public class RpcClientRemoting extends RpcRemoting {

    public RpcClientRemoting(ClientConnectionManager connectionManager) {
        super(connectionManager);
    }

    public <R> R syncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws InterruptedException, RemotingException {

        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);

        return this.syncCall(request, connection, timeoutMills);
    }

    public <R> RpcInvokeFuture<R> asyncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws RemotingException {

        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);

        return super.asyncCall(request, connection, timeoutMills);
    }

    public <R> void asyncCall(Object request, Endpoint endpoint,
                              int timeoutMills,
                              RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {

        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);

        super.asyncCall(request, connection, timeoutMills, rpcInvokeCallBack);
    }

    public void oneway(Object request, Endpoint endpoint) throws RemotingException {

        Connection connection = connectionManager.getOrCreateIfAbsent(endpoint);
        connectionManager.check(connection);

        super.oneway(request, connection);
    }

}
