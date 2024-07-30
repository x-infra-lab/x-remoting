package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.ConnectionClosedException;
import io.github.xinfra.lab.remoting.exception.SendMessageException;
import io.github.xinfra.lab.remoting.exception.TimeoutException;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class RpcMessageFactoryTest {

    @Test
    public void testCreateRequestMessage() {
        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();

        RpcRequestMessage requestMessage = rpcMessageFactory.createRequestMessage();
        Assertions.assertNotNull(requestMessage);
        Assertions.assertEquals(requestMessage.messageType(), MessageType.request);
        Assertions.assertEquals(requestMessage.protocolType(), RpcProtocol.RPC);
        Assertions.assertEquals(requestMessage.serializationType(), SerializationType.HESSION);
    }

    @Test
    public void testCreateSendFailResponseMessage() throws UnknownHostException {
        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();
        InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("localhost", 8080);

        RpcResponseMessage responseMessage = rpcMessageFactory.createSendFailResponseMessage(IDGenerator.nextRequestId(),
                new RuntimeException("testCreateSendFailResponseMessage"), remoteAddress);
        Assertions.assertNotNull(responseMessage);
        Assertions.assertEquals(responseMessage.getStatus(), ResponseStatus.CLIENT_SEND_ERROR.getCode());
        Assertions.assertTrue(responseMessage.getCause() instanceof SendMessageException);
        Assertions.assertEquals(responseMessage.messageType(), MessageType.response);
        Assertions.assertEquals(responseMessage.protocolType(), RpcProtocol.RPC);
        Assertions.assertEquals(responseMessage.serializationType(), SerializationType.HESSION);
    }

    @Test
    public void testCreateTimeoutResponseMessage() throws UnknownHostException {
        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();
        InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("localhost", 8080);

        RpcResponseMessage responseMessage = rpcMessageFactory.createTimeoutResponseMessage(IDGenerator.nextRequestId(),
                remoteAddress);
        Assertions.assertNotNull(responseMessage);
        Assertions.assertEquals(responseMessage.getStatus(), ResponseStatus.TIMEOUT.getCode());
        Assertions.assertTrue(responseMessage.getCause() instanceof TimeoutException);
        Assertions.assertEquals(responseMessage.messageType(), MessageType.response);
        Assertions.assertEquals(responseMessage.protocolType(), RpcProtocol.RPC);
    }

    @Test
    public void testCreateConnectionClosedMessage() throws UnknownHostException {
        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();
        InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("localhost", 8080);

        RpcResponseMessage responseMessage = rpcMessageFactory.createConnectionClosedMessage(IDGenerator.nextRequestId(),
                remoteAddress);
        Assertions.assertNotNull(responseMessage);
        Assertions.assertEquals(responseMessage.getStatus(), ResponseStatus.CONNECTION_CLOSED.getCode());
        Assertions.assertTrue(responseMessage.getCause() instanceof ConnectionClosedException);
        Assertions.assertEquals(responseMessage.messageType(), MessageType.response);
        Assertions.assertEquals(responseMessage.protocolType(), RpcProtocol.RPC);
    }

    @Test
    public void testCreateHeartbeatRequestMessage() {
        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();

        RpcRequestMessage requestMessage = rpcMessageFactory.createHeartbeatRequestMessage();
        Assertions.assertNotNull(requestMessage);
        Assertions.assertEquals(requestMessage.messageType(), MessageType.heartbeatRequest);
        Assertions.assertEquals(requestMessage.protocolType(), RpcProtocol.RPC);
    }
}
