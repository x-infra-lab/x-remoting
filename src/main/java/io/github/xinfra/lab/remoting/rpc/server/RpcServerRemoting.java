package io.github.xinfra.lab.remoting.rpc.server;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ServerConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.RpcRemoting;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeCallBack;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeFuture;

public class RpcServerRemoting extends RpcRemoting {
    public RpcServerRemoting(ServerConnectionManager connectionManager) {
        super(connectionManager);
    }

    public <R> R syncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws InterruptedException, RemotingException {

        Connection connection = connectionManager.get(endpoint);
        if (null == connection) {
            throw new RemotingException("Client address [" + endpoint
                    + "] not connected yet!");
        }
        connectionManager.check(connection);

        return this.syncCall(request, connection, timeoutMills);
    }

    public <R> RpcInvokeFuture<R> asyncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws RemotingException {

        Connection connection = connectionManager.get(endpoint);
        if (null == connection) {
            throw new RemotingException("Client address [" + endpoint
                    + "] not connected yet!");
        }

        connectionManager.check(connection);

        return super.asyncCall(request, connection, timeoutMills);
    }

    public <R> void asyncCall(Object request, Endpoint endpoint,
                              int timeoutMills,
                              RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {

        Connection connection = connectionManager.get(endpoint);
        if (null == connection) {
            throw new RemotingException("Client address [" + endpoint
                    + "] not connected yet!");
        }
        connectionManager.check(connection);

        super.asyncCall(request, connection, timeoutMills, rpcInvokeCallBack);
    }

    public void oneway(Object request, Endpoint endpoint) throws RemotingException {

        Connection connection = connectionManager.get(endpoint);
        if (null == connection) {
            throw new RemotingException("Client address [" + endpoint
                    + "] not connected yet!");
        }

        connectionManager.check(connection);

        super.oneway(request, connection);
    }
}
