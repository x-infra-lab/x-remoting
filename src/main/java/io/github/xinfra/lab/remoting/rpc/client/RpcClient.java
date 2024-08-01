package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class RpcClient extends AbstractLifeCycle {
    private RpcProtocol protocol;
    private RpcClientRemoting rpcClientRemoting;
    private ClientConnectionManager connectionManager;
    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors = new ConcurrentHashMap<>();


    public RpcClient() {
        this.protocol = new RpcProtocol();
        this.connectionManager = new ClientConnectionManager(protocol, userProcessors);
        rpcClientRemoting = new RpcClientRemoting(protocol, connectionManager);
    }

    @Override
    public void startup() {
        super.startup();
        connectionManager.startup();
        rpcClientRemoting.startup();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        connectionManager.shutdown();
        rpcClientRemoting.shutdown();
    }

    public <R> R syncCall(Object request, SocketAddress socketAddress, int timeoutMills)
            throws RemotingException, InterruptedException {

        return rpcClientRemoting.syncCall(request, socketAddress, timeoutMills);
    }

    public <R> RpcInvokeFuture<R> asyncCall(Object request, SocketAddress socketAddress, int timeoutMills)
            throws RemotingException {
        return rpcClientRemoting.asyncCall(request, socketAddress, timeoutMills);
    }

    public <R> void asyncCall(Object request, SocketAddress socketAddress, int timeoutMills,
                              RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
        rpcClientRemoting.asyncCall(request, socketAddress, timeoutMills, rpcInvokeCallBack);
    }

    public void oneway(Object request, SocketAddress socketAddress) throws RemotingException {
        rpcClientRemoting.oneway(request, socketAddress);
    }

    public void registerUserProcessor(UserProcessor<?> userProcessor) {
        UserProcessor<?> oldUserProcessor = userProcessors.putIfAbsent(userProcessor.interest(), userProcessor);
        if (oldUserProcessor != null) {
            String msg= "interest key:" + userProcessor.interest() + " has already been registered to rpc client.";
            throw new RuntimeException(msg);
        }
    }
}
