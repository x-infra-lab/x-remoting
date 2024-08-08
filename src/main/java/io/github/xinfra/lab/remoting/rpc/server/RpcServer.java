package io.github.xinfra.lab.remoting.rpc.server;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeCallBack;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeFuture;
import io.github.xinfra.lab.remoting.server.BaseRemotingServer;
import lombok.Getter;

import java.io.IOException;
import java.net.SocketAddress;

public class RpcServer extends BaseRemotingServer {
    @Getter
    private RpcProtocol protocol;
    @Getter
    private RpcServerRemoting rpcServerRemoting;

    public RpcServer(RpcServerConfig config) {
        super(config);
    }

    @Override
    public void startup() {
        super.startup();
        protocol = new RpcProtocol();
        rpcServerRemoting = new RpcServerRemoting(protocol, connectionManager);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            protocol.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public <R> R syncCall(Object request, SocketAddress socketAddress, int timeoutMills)
            throws InterruptedException, RemotingException {
        ensureStarted();

        return rpcServerRemoting.syncCall(request, socketAddress, timeoutMills);
    }

    public <R> RpcInvokeFuture<R> asyncCall(Object request, SocketAddress socketAddress, int timeoutMills)
            throws RemotingException {
        ensureStarted();

        return rpcServerRemoting.asyncCall(request, socketAddress, timeoutMills);
    }

    public <R> void asyncCall(Object request, SocketAddress socketAddress,
                              int timeoutMills,
                              RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
        ensureStarted();

        rpcServerRemoting.asyncCall(request, socketAddress, timeoutMills, rpcInvokeCallBack);
    }

    public void oneway(Object request, SocketAddress socketAddress) throws RemotingException {
        ensureStarted();

        rpcServerRemoting.oneway(request, socketAddress);
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }
}
