package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.message.RpcStatusCode;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.RpcProtocol;


public class RpcRemoting extends BaseRemoting {

    static {
        ProtocolManager.registerProtocol(ProtocolType.RPC, new RpcProtocol());
    }

    private RpcMessageFactory rpcMessageFactory;

    private ConnectionManager connectionManager;

    public RpcRemoting(ConnectionManager connectionManager) {
        super(ProtocolManager.getProtocol(ProtocolType.RPC).messageFactory());
        this.rpcMessageFactory = (RpcMessageFactory) ProtocolManager.getProtocol(ProtocolType.RPC).messageFactory();
        this.connectionManager = connectionManager;
    }

    public <R> R syncCall(Object request, Endpoint endpoint, int timeoutMills) throws InterruptedException {
        RpcRequestMessage requestMessage = rpcMessageFactory.createRequestMessage();
        requestMessage.setContent(request);
        requestMessage.setContentType(RpcRequestMessage.class.getName());

        Connection connection = connectionManager.getConnection(endpoint);
        // TODO check connection ??

        // TODO FIXME
        RpcResponseMessage responseMessage = (RpcResponseMessage) super.syncCall(requestMessage, connection, timeoutMills);
        if (responseMessage.getStatus() != RpcStatusCode.SUCCESS) {
            Object result = responseMessage.getContent();
            if (result instanceof Throwable) {
                throw new RemotingException((Throwable) result);
            }
            throw new RemotingException("Remoting fail. unknown exception");
        }
        return (R) responseMessage.getContent();
    }
}
