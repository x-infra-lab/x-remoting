package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponses;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;

import java.util.concurrent.TimeUnit;

public class RpcInvokeFuture<T> {
    private InvokeFuture invokeFuture;

    public RpcInvokeFuture(InvokeFuture invokeFuture) {
        this.invokeFuture = invokeFuture;
    }


    public <T> T get() throws InterruptedException, RemotingException {
        RpcResponseMessage responseMessage = (RpcResponseMessage) invokeFuture.await();
        return RpcResponses.getResponseObject(responseMessage);
    }

    public <T> T get(long timeout, TimeUnit unit) throws InterruptedException, RemotingException {
        RpcResponseMessage responseMessage = (RpcResponseMessage) invokeFuture.await(timeout, unit);
        return RpcResponses.getResponseObject(responseMessage);
    }
}