package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.ConnectionClosedException;
import io.github.xinfra.lab.remoting.exception.SendMessageException;
import io.github.xinfra.lab.remoting.exception.TimeoutException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.exception.RpcServerException;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class RpcMessageFactoryTest {

	@Test
	public void testCreateRequestMessage() {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

		RemotingRequestMessage requestMessage = remotingMessageFactory.createRequestMessage();
		Assertions.assertNotNull(requestMessage);
		Assertions.assertEquals(requestMessage.messageType(), RpcMessageType.request);
		Assertions.assertArrayEquals(requestMessage.protocolIdentifier(), RemotingProtocol.PROTOCOL_CODE);
		Assertions.assertEquals(requestMessage.serializationType(), SerializationType.HESSION);
	}

	@Test
	public void testCreateSendFailResponseMessage() throws UnknownHostException {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();
		InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("localhost", 8080);

		RemotingResponseMessage responseMessage = remotingMessageFactory.createSendFailedResponseMessage(
				IDGenerator.nextRequestId(), new RuntimeException("testCreateSendFailResponseMessage"), remoteAddress);
		Assertions.assertNotNull(responseMessage);
		Assertions.assertEquals(responseMessage.getStatus(), ResponseStatus.CLIENT_SEND_ERROR.getCode());
		Assertions.assertTrue(responseMessage.getCause() instanceof SendMessageException);
		Assertions.assertEquals(responseMessage.messageType(), RpcMessageType.response);
		Assertions.assertArrayEquals(responseMessage.protocolIdentifier(), RemotingProtocol.PROTOCOL_CODE);
		Assertions.assertEquals(responseMessage.serializationType(), SerializationType.HESSION);
	}

	@Test
	public void testCreateTimeoutResponseMessage() throws UnknownHostException {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();
		InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("localhost", 8080);

		RemotingResponseMessage responseMessage = remotingMessageFactory.createTimeoutResponseMessage(IDGenerator.nextRequestId(),
				remoteAddress);
		Assertions.assertNotNull(responseMessage);
		Assertions.assertEquals(responseMessage.getStatus(), ResponseStatus.TIMEOUT.getCode());
		Assertions.assertTrue(responseMessage.getCause() instanceof TimeoutException);
		Assertions.assertEquals(responseMessage.messageType(), RpcMessageType.response);
		Assertions.assertArrayEquals(responseMessage.protocolIdentifier(), RemotingProtocol.PROTOCOL_CODE);
	}

	@Test
	public void testCreateConnectionClosedMessage() throws UnknownHostException {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();
		InetSocketAddress remoteAddress = InetSocketAddress.createUnresolved("localhost", 8080);

		RemotingResponseMessage responseMessage = remotingMessageFactory
			.createConnectionClosedMessage(IDGenerator.nextRequestId(), remoteAddress);
		Assertions.assertNotNull(responseMessage);
		Assertions.assertEquals(responseMessage.getStatus(), ResponseStatus.CONNECTION_CLOSED.getCode());
		Assertions.assertTrue(responseMessage.getCause() instanceof ConnectionClosedException);
		Assertions.assertEquals(responseMessage.messageType(), RpcMessageType.response);
		Assertions.assertArrayEquals(responseMessage.protocolIdentifier(), RemotingProtocol.PROTOCOL_CODE);
	}

	@Test
	public void testCreateHeartbeatRequestMessage() {
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();

		RemotingRequestMessage requestMessage = remotingMessageFactory.createHeartbeatRequestMessage();
		Assertions.assertNotNull(requestMessage);
		Assertions.assertEquals(requestMessage.messageType(), RpcMessageType.heartbeatRequest);
		Assertions.assertArrayEquals(requestMessage.protocolIdentifier(), RemotingProtocol.PROTOCOL_CODE);
	}

	@Test
	public void testCreateResponse1() {
		String responseContent = "this is response content";
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();
		RemotingResponseMessage response = remotingMessageFactory.createResponse(IDGenerator.nextRequestId(), responseContent);

		Assertions.assertNotNull(response);
		Assertions.assertEquals(response.messageType(), RpcMessageType.response);
		Assertions.assertArrayEquals(response.protocolIdentifier(), RemotingProtocol.PROTOCOL_CODE);
		Assertions.assertEquals(response.getStatus(), ResponseStatus.SUCCESS.getCode());
		Assertions.assertEquals(response.getContent(), responseContent);
	}

	@Test
	public void testCreateExceptionResponse1() {
		String errorMsg = "testCreateExceptionResponse1";
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();
		RemotingResponseMessage response = remotingMessageFactory.createExceptionResponse(IDGenerator.nextRequestId(), errorMsg);

		Assertions.assertNotNull(response);
		Assertions.assertEquals(response.messageType(), RpcMessageType.response);
		Assertions.assertArrayEquals(response.protocolIdentifier(), RemotingProtocol.PROTOCOL_CODE);
		Assertions.assertEquals(response.getStatus(), ResponseStatus.SERVER_EXCEPTION.getCode());
		Assertions.assertInstanceOf(RpcServerException.class, response.getContent());
	}

	@Test
	public void testCreateExceptionResponse2() {
		String errorMsg = "testCreateExceptionResponse2";
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();
		RemotingResponseMessage response = remotingMessageFactory.createExceptionResponse(IDGenerator.nextRequestId(),
				new RuntimeException("testCreateExceptionResponse2"), errorMsg);

		Assertions.assertNotNull(response);
		Assertions.assertEquals(response.messageType(), RpcMessageType.response);
		Assertions.assertArrayEquals(response.protocolIdentifier(), RemotingProtocol.PROTOCOL_CODE);
		Assertions.assertEquals(response.getStatus(), ResponseStatus.SERVER_EXCEPTION.getCode());
		Assertions.assertInstanceOf(RpcServerException.class, response.getContent());
	}

}
