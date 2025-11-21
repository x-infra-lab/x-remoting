package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.impl.RemotingProtocolIdentifier;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

public class RemotingMessageFactoryTest {

    @Test
    public void testCreateRequestMessage() {
        RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

        RemotingRequestMessage requestMessage = remotingMessageFactory.createRequest(IDGenerator.nextRequestId(), SerializationType.Hession);
        Assertions.assertNotNull(requestMessage);
        Assertions.assertEquals(requestMessage.messageType(), MessageType.request);
        Assertions.assertArrayEquals(requestMessage.protocolIdentifier().code(), RemotingProtocolIdentifier.PROTOCOL_CODE);
        Assertions.assertEquals(requestMessage.serializationType(), SerializationType.Hession);
        Assertions.assertNull(requestMessage.getPath());
        Assertions.assertNull(requestMessage.headers());
        Assertions.assertNull(requestMessage.body());
    }

    @Test
    public void testCreateSendFailResponseMessage() throws UnknownHostException {
        RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

        RemotingResponseMessage responseMessage = remotingMessageFactory.createResponse(
                IDGenerator.nextRequestId(), SerializationType.Hession, ResponseStatus.SendFailed,
                new UnknownHostException("testCreateSendFailResponseMessage"));

        Assertions.assertNotNull(responseMessage);
        Assertions.assertEquals(responseMessage.messageType(), MessageType.response);
        Assertions.assertEquals(responseMessage.responseStatus(), ResponseStatus.SendFailed);
        Assertions.assertEquals(responseMessage.serializationType(), SerializationType.Hession);
        Assertions.assertArrayEquals(responseMessage.protocolIdentifier().code(), RemotingProtocolIdentifier.PROTOCOL_CODE);

        Assertions.assertNull(responseMessage.headers());
        Assertions.assertTrue(responseMessage.body().getBodyValue() instanceof UnknownHostException);
    }

    @Test
    public void testCreateTimeoutResponseMessage() {
        RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

        RemotingResponseMessage responseMessage = remotingMessageFactory
                .createResponse(IDGenerator.nextRequestId(), SerializationType.Hession, ResponseStatus.Timeout);
        Assertions.assertNotNull(responseMessage);
        Assertions.assertEquals(responseMessage.responseStatus(), ResponseStatus.Timeout);
        Assertions.assertEquals(responseMessage.body(), null);

    }

    @Test
    public void testCreateConnectionClosedMessage() throws UnknownHostException {
        RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

        RemotingResponseMessage responseMessage = remotingMessageFactory
                .createResponse(IDGenerator.nextRequestId(), SerializationType.Hession, ResponseStatus.ConnectionClosed);
        Assertions.assertNotNull(responseMessage);
        Assertions.assertEquals(responseMessage.responseStatus(), ResponseStatus.ConnectionClosed);
        Assertions.assertEquals(responseMessage.body(), null);
    }

    @Test
    public void testCreateHeartbeatRequestMessage() {
        RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

        RemotingRequestMessage requestMessage = remotingMessageFactory.createHeartbeatRequest(IDGenerator.nextRequestId(),
                SerializationType.Hession);
        Assertions.assertNotNull(requestMessage);
        Assertions.assertEquals(requestMessage.messageType(), MessageType.heartbeatRequest);
        Assertions.assertArrayEquals(requestMessage.protocolIdentifier().code(), RemotingProtocolIdentifier.PROTOCOL_CODE);
        Assertions.assertNull(requestMessage.getPath());
        Assertions.assertNull(requestMessage.headers());
        Assertions.assertNull(requestMessage.body());
    }

}
