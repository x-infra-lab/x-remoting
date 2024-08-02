package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.rpc.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponses;


public class RpcRemoting extends BaseRemoting {


    public RpcRemoting(Protocol protocol) {
        super(protocol);
    }

    public <R> R syncCall(Object request, Connection connection, int timeoutMills)
            throws InterruptedException, RemotingException {
        RpcRequestMessage requestMessage = buildRequestMessage(request);

        RpcResponseMessage responseMessage = (RpcResponseMessage) super.syncCall(requestMessage, connection, timeoutMills);
        return RpcResponses.getResponseObject(responseMessage);
    }

    public <R> RpcInvokeFuture<R> asyncCall(Object request, Connection connection, int timeoutMills)
            throws RemotingException {
        RpcRequestMessage requestMessage = buildRequestMessage(request);

        InvokeFuture<?> invokeFuture = super.asyncCall(requestMessage, connection, timeoutMills);
        return new RpcInvokeFuture<R>(invokeFuture);
    }

    public <R> void asyncCall(Object request, Connection connection,
                              int timeoutMills,
                              RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
        RpcRequestMessage requestMessage = buildRequestMessage(request);

        super.asyncCall(requestMessage, connection, timeoutMills, rpcInvokeCallBack);
    }

    public void oneway(Object request, Connection connection) throws RemotingException {
        RpcRequestMessage requestMessage = buildRequestMessage(request);

        super.oneway(requestMessage, connection);
    }

    private RpcRequestMessage buildRequestMessage(Object request) throws SerializeException {
        RpcRequestMessage requestMessage = messageFactory.createRequestMessage();
        requestMessage.setContent(request);
        requestMessage.setContentType(request.getClass().getName());

        requestMessage.serialize();
        return requestMessage;
    }
}
