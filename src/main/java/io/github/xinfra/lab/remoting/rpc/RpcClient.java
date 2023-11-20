package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.client.InvokeCallBack;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;

import java.util.concurrent.Future;

public class RpcClient extends AbstractLifeCycle {

    private RpcRemoting rpcRemoting;

    @Override
    public void startup() {
        super.startup();
        // TODO
    }

    @Override
    public void shutdown() {
        super.shutdown();
        // TODO
    }

    public <R> R syncCall(Object request, Endpoint endpoint) {
        // todo
        return null;
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
