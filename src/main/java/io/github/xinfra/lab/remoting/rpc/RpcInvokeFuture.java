package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

public class RpcInvokeFuture<T> {
    private InvokeFuture invokeFuture;

    public RpcInvokeFuture(InvokeFuture invokeFuture) {
        this.invokeFuture = invokeFuture;
    }


    public <T> T get() throws InterruptedException, RemotingException {
        RpcResponseMessage responseMessage = (RpcResponseMessage) invokeFuture.await();
        SocketAddress remoteAddress = invokeFuture.getConnection().getChannel().remoteAddress();
        return RpcResponses.getResponseObject(responseMessage, remoteAddress);
    }

    public <T> T get(long timeout, TimeUnit unit) throws InterruptedException, RemotingException {
        RpcResponseMessage responseMessage = (RpcResponseMessage) invokeFuture.await(timeout, unit);
        SocketAddress remoteAddress = invokeFuture.getConnection().getChannel().remoteAddress();
        return RpcResponses.getResponseObject(responseMessage, remoteAddress);
    }
}