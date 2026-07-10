package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.rpc.client.IDGenerator;
import io.github.xinfra.lab.remoting.rpc.protocol.RemotingProtocolId;
import io.github.xinfra.lab.remoting.rpc.message.MessageType;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

public class RemotingMessageFactoryTest {

	@Test
	public void testCreateRequestMessage() {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

		RemotingRequestMessage requestMessage = remotingMessageFactory.createRequest(new IDGenerator().nextRequestId(),
				SerializationType.Hessian);
		Assertions.assertNotNull(requestMessage);
		Assertions.assertEquals(requestMessage.getMessageType(), MessageType.request);
		Assertions.assertArrayEquals(requestMessage.getProtocolIdentifier().getCodes(),
				RemotingProtocolId.PROTOCOL_CODE);
		Assertions.assertEquals(requestMessage.getSerializationType(), SerializationType.Hessian);
		Assertions.assertNull(requestMessage.getPath());
		Assertions.assertNull(requestMessage.getHeaders());
		Assertions.assertNull(requestMessage.getBody());
	}

	@Test
	public void testCreateSendFailResponseMessage() throws UnknownHostException {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

		RemotingResponseMessage responseMessage = remotingMessageFactory.createResponse(
				new IDGenerator().nextRequestId(), SerializationType.Hessian, ResponseStatus.SendFailed,
				new UnknownHostException("testCreateSendFailResponseMessage"));

		Assertions.assertNotNull(responseMessage);
		Assertions.assertEquals(responseMessage.getMessageType(), MessageType.response);
		Assertions.assertEquals(responseMessage.getResponseStatus(), ResponseStatus.SendFailed);
		Assertions.assertEquals(responseMessage.getSerializationType(), SerializationType.Hessian);
		Assertions.assertArrayEquals(responseMessage.getProtocolIdentifier().getCodes(),
				RemotingProtocolId.PROTOCOL_CODE);

		Assertions.assertNull(responseMessage.getHeaders());
		Assertions.assertTrue(responseMessage.getBody().getBodyValue() instanceof UnknownHostException);
	}

	@Test
	public void testCreateTimeoutResponseMessage() {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

		RemotingResponseMessage responseMessage = remotingMessageFactory
			.createResponse(new IDGenerator().nextRequestId(), SerializationType.Hessian, ResponseStatus.Timeout);
		Assertions.assertNotNull(responseMessage);
		Assertions.assertEquals(responseMessage.getResponseStatus(), ResponseStatus.Timeout);
		Assertions.assertEquals(responseMessage.getBody(), null);

	}

	@Test
	public void testCreateConnectionClosedMessage() throws UnknownHostException {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

		RemotingResponseMessage responseMessage = remotingMessageFactory.createResponse(
				new IDGenerator().nextRequestId(), SerializationType.Hessian, ResponseStatus.ConnectionClosed);
		Assertions.assertNotNull(responseMessage);
		Assertions.assertEquals(responseMessage.getResponseStatus(), ResponseStatus.ConnectionClosed);
		Assertions.assertEquals(responseMessage.getBody(), null);
	}

	@Test
	public void testCreateHeartbeatRequestMessage() {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

		RemotingRequestMessage requestMessage = remotingMessageFactory
			.createHeartbeatRequest(new IDGenerator().nextRequestId(), SerializationType.Hessian);
		Assertions.assertNotNull(requestMessage);
		Assertions.assertEquals(requestMessage.getMessageType(), MessageType.heartbeatRequest);
		Assertions.assertArrayEquals(requestMessage.getProtocolIdentifier().getCodes(),
				RemotingProtocolId.PROTOCOL_CODE);
		Assertions.assertNull(requestMessage.getPath());
		Assertions.assertNull(requestMessage.getHeaders());
		Assertions.assertNull(requestMessage.getBody());
	}

}
