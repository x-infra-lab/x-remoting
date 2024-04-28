package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.RpcProtocol;


public class RpcRemoting extends BaseRemoting {

    static {
        ProtocolManager.registerProtocolIfAbsent(ProtocolType.RPC, new RpcProtocol());
    }

    protected RpcMessageFactory rpcMessageFactory;

    protected ConnectionManager connectionManager;

    public RpcRemoting(ConnectionManager connectionManager) {
        super(ProtocolManager.getProtocol(ProtocolType.RPC).messageFactory());
        this.rpcMessageFactory = (RpcMessageFactory) ProtocolManager.getProtocol(ProtocolType.RPC).messageFactory();
        this.connectionManager = connectionManager;
    }

    public <R> R syncCall(Object request, Connection connection, int timeoutMills)
            throws InterruptedException, RemotingException {
        RpcRequestMessage requestMessage = buildRequestMessage(request);

        RpcResponseMessage responseMessage = (RpcResponseMessage) super.syncCall(requestMessage, connection, timeoutMills);
        return RpcResponses.getResponseObject(responseMessage, connection.getChannel().remoteAddress());
    }

    public <R> RpcInvokeFuture<R> asyncCall(Object request, Connection connection, int timeoutMills)
            throws RemotingException {
        RpcRequestMessage requestMessage = buildRequestMessage(request);

        InvokeFuture invokeFuture = super.asyncCall(requestMessage, connection, timeoutMills);
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
        RpcRequestMessage requestMessage = rpcMessageFactory.createRequestMessage();
        requestMessage.setContent(request);
        requestMessage.setContentType(request.getClass().getName());

        requestMessage.serialize();
        return requestMessage;
    }
}
