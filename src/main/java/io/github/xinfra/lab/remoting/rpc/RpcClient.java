package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class RpcClient extends AbstractLifeCycle {

    private RpcClientRemoting rpcClientRemoting;
    private ClientConnectionManager connectionManager;
    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors = new ConcurrentHashMap<>();


    public RpcClient() {
        this.connectionManager = new ClientConnectionManager(userProcessors);
        rpcClientRemoting = new RpcClientRemoting(connectionManager);
    }

    @Override
    public void startup() {
        super.startup();
    }

    @Override
    public void shutdown() {
        // todo close connectionManager
        super.shutdown();
    }

    public <R> R syncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws RemotingException, InterruptedException {

        return rpcClientRemoting.syncCall(request, endpoint, timeoutMills);
    }

    public <R> RpcInvokeFuture<R> asyncCall(Object request, Endpoint endpoint, int timeoutMills)
            throws RemotingException {
        return rpcClientRemoting.asyncCall(request, endpoint, timeoutMills);
    }

    public <R> void asyncCall(Object request, Endpoint endpoint, int timeoutMills,
                              RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
        rpcClientRemoting.asyncCall(request, endpoint, timeoutMills, rpcInvokeCallBack);
    }

    public void oneway(Object request, Endpoint endpoint) throws RemotingException {
        rpcClientRemoting.oneway(request, endpoint);
    }

    public void registerUserProcessor(UserProcessor<?> userProcessor) {
        UserProcessor<?> oldUserProcessor = userProcessors.put(userProcessor.interest(), userProcessor);
        if (oldUserProcessor != null) {
            log.warn("registered userProcessor change from:{} to:{}", oldUserProcessor, userProcessor);
        }
    }
}
