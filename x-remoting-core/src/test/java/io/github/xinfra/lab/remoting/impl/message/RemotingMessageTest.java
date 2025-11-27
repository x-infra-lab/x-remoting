package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.DefaultMessageHeaders;
import io.github.xinfra.lab.remoting.message.MessageHeaders;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RemotingMessageTest {

	@Test
	public void testRpcRequestSerialize() throws SerializeException, DeserializeException {
		String content = "this is rpc content";
		DefaultMessageHeaders header = new DefaultMessageHeaders();

		MessageHeaders.StringKey headerKey = MessageHeaders.Key.stringKey("test-key");
		String headerValue = "test-value";
		header.put(headerKey, headerValue);
		Integer requestId = IDGenerator.nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hession);
		requestMessage.setPath("/test");
		requestMessage.setHeaders(header);
		requestMessage.setBody(new RemotingMessageBody(content));

		requestMessage.serialize();
		Assertions.assertNotNull(requestMessage.getPathData());
		Assertions.assertNotNull(requestMessage.getHeaders().getData());
		Assertions.assertNotNull(requestMessage.getBody().getData());

		// deserialize
		RemotingRequestMessage requestMessage2 = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hession);
		requestMessage2.setPathData(requestMessage.getPathData());

		byte[] headerBytes = new byte[0];
		requestMessage.getHeaders().getData().forEach(data -> ArrayUtils.addAll(headerBytes, data));
		requestMessage2.setHeaders(new DefaultMessageHeaders(headerBytes));

		byte[] bodyBytes = new byte[0];
		requestMessage.getBody().getData().forEach(data -> ArrayUtils.addAll(bodyBytes, data));
		requestMessage2.setBody(new RemotingMessageBody(bodyBytes));
		requestMessage2.deserialize();

		Assertions.assertEquals(requestMessage2.getPath(), requestMessage.getPath());
		Assertions.assertEquals(requestMessage2.getHeaders().get(headerKey), headerValue);
		Assertions.assertEquals(requestMessage2.getBody().getBodyValue(), content);
	}

	@Test
	public void testRpcResponse1() throws SerializeException, DeserializeException {
		String content = "this is rpc content";
		DefaultMessageHeaders header = new DefaultMessageHeaders();
		MessageHeaders.StringKey headerKey = MessageHeaders.Key.stringKey("test-key");
		String headerValue = "test-value";
		header.put(headerKey, headerValue);

		Integer requestId = IDGenerator.nextRequestId();
		RemotingResponseMessage responseMessage = new RemotingResponseMessage(requestId, SerializationType.Hession,
				ResponseStatus.OK);

		responseMessage.setHeaders(header);
		responseMessage.setBody(new RemotingMessageBody(content));
		responseMessage.serialize();

		Assertions.assertNotNull(responseMessage.getHeaders().getData());
		Assertions.assertNotNull(responseMessage.getBody().getData());

		RemotingResponseMessage responseMessage2 = new RemotingResponseMessage(requestId, SerializationType.Hession,
				ResponseStatus.OK);

		byte[] headerBytes = new byte[0];
		responseMessage.getHeaders().getData().forEach(data -> ArrayUtils.addAll(headerBytes, data));
		responseMessage2.setHeaders(new DefaultMessageHeaders(headerBytes));

		byte[] bodyBytes = new byte[0];
		responseMessage.getBody().getData().forEach(data -> ArrayUtils.addAll(bodyBytes, data));
		responseMessage2.setBody(new RemotingMessageBody(bodyBytes));
		responseMessage2.deserialize();

		Assertions.assertEquals(responseMessage2.getHeaders().get(headerKey), headerValue);
		Assertions.assertEquals(responseMessage2.getBody().getBodyValue(), content);
	}

	@Test
	public void testExceptionRpcResponse1() throws SerializeException, DeserializeException {
		int requestId = IDGenerator.nextRequestId();
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();
		RemotingResponseMessage responseMessage = remotingMessageFactory.createResponse(requestId,
				SerializationType.Hession, ResponseStatus.Error, new RuntimeException("testCreateExceptionResponse1"));
		responseMessage.serialize();

		Assertions.assertNull(responseMessage.getHeaders());
		Assertions.assertNotNull(responseMessage.getBody().getData());

		RemotingResponseMessage responseMessage2 = new RemotingResponseMessage(requestId, SerializationType.Hession,
				ResponseStatus.OK);
		responseMessage2.setBody(new RemotingMessageBody(responseMessage.getBody().getData()));
		responseMessage2.deserialize();

		Assertions.assertTrue(responseMessage2.getBody().getBodyValue() instanceof RuntimeException);
	}

}
