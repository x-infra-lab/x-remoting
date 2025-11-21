package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.impl.exception.RemotingServerException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RemotingMessageTest {

	@Test
	public void testRpcRequest1() throws SerializeException, DeserializeException {
		String content = "this is rpc content";
		String contentType = content.getClass().getName();
		RemotingMessageHeaders header = new RemotingMessageHeaders();
		header.addItem(new RemotingMessageHeaders.Item("this is header key", "this is header value"));

		Integer requestId = IDGenerator.nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId);

		requestMessage.setHeaders(header);
		requestMessage.setContent(content);
		requestMessage.setContentType(contentType);

		requestMessage.serialize();

		Assertions.assertNotNull(requestMessage.getContentTypeData());
		Assertions.assertEquals(requestMessage.getContentTypeData().length, requestMessage.getContentTypeLength());

		Assertions.assertNotNull(requestMessage.getHeaderData());
		Assertions.assertEquals(requestMessage.getHeaderData().length, requestMessage.getHeaderLength());

		Assertions.assertNotNull(requestMessage.getContentData());
		Assertions.assertEquals(requestMessage.getContentData().length, requestMessage.getContentLength());

		RemotingRequestMessage requestMessage2 = new RemotingRequestMessage(requestId);
		requestMessage2.setContentTypeData(requestMessage.getContentTypeData());
		requestMessage2.setHeaderData(requestMessage.getHeaderData());
		requestMessage2.setContentData(requestMessage.getContentData());
		requestMessage2.deserialize();

		Assertions.assertEquals(requestMessage2.getContentType(), requestMessage.getContentType());
		Assertions.assertEquals(requestMessage2.getHeader(), requestMessage.getHeader());
		Assertions.assertEquals(requestMessage2.getContent(), requestMessage.getContent());
	}

	@Test
	public void testRpcResponse1() throws SerializeException, DeserializeException {
		String content = "this is rpc content";
		String contentType = content.getClass().getName();
		RemotingMessageHeaders header = new RemotingMessageHeaders();
		header.addItem(new RemotingMessageHeaders.Item("this is header key", "this is header value"));

		Integer requestId = IDGenerator.nextRequestId();
		RemotingResponseMessage responseMessage = new RemotingResponseMessage(requestId);

		responseMessage.setHeaders(header);
		responseMessage.setContent(content);
		responseMessage.setContentType(contentType);
		responseMessage.setStatus(ResponseStatus.SUCCESS.getCode());

		responseMessage.serialize();

		Assertions.assertNotNull(responseMessage.getContentTypeData());
		Assertions.assertEquals(responseMessage.getContentTypeData().length, responseMessage.getContentTypeLength());

		Assertions.assertNotNull(responseMessage.getHeaderData());
		Assertions.assertEquals(responseMessage.getHeaderData().length, responseMessage.getHeaderLength());

		Assertions.assertNotNull(responseMessage.getContentData());
		Assertions.assertEquals(responseMessage.getContentData().length, responseMessage.getContentLength());

		RemotingResponseMessage responseMessage2 = new RemotingResponseMessage(requestId);
		responseMessage2.setContentTypeData(responseMessage.getContentTypeData());
		responseMessage2.setHeaderData(responseMessage.getHeaderData());
		responseMessage2.setContentData(responseMessage.getContentData());

		responseMessage2.deserialize(DeserializeLevel.CONTENT_TYPE);
		Assertions.assertNotNull(responseMessage2.getContentType());
		Assertions.assertNull(responseMessage2.getHeader());
		Assertions.assertNull(responseMessage2.getContent());

		responseMessage2.deserialize(DeserializeLevel.HEADER);
		Assertions.assertNotNull(responseMessage2.getContentType());
		Assertions.assertNotNull(responseMessage2.getHeader());
		Assertions.assertNull(responseMessage2.getContent());

		responseMessage2.deserialize(DeserializeLevel.ALL);

		Assertions.assertEquals(responseMessage2.getContentType(), responseMessage.getContentType());
		Assertions.assertEquals(responseMessage2.getHeader(), responseMessage.getHeader());
		Assertions.assertEquals(responseMessage2.getContent(), responseMessage.getContent());
	}

	@Test
	public void testExceptionRpcResponse1() throws SerializeException, DeserializeException {
		int requestId = IDGenerator.nextRequestId();
		RemotingMessageFactory remotingMessageFactory = new RemotingMessageFactory();
		RemotingResponseMessage responseMessage = remotingMessageFactory.createExceptionResponse(requestId,
				new RuntimeException("testCreateExceptionResponse1"), ResponseStatus.SERVER_DESERIAL_EXCEPTION);

		responseMessage.serialize();

		Assertions.assertNotNull(responseMessage.getContentTypeData());
		Assertions.assertEquals(responseMessage.getContentTypeData().length, responseMessage.getContentTypeLength());

		Assertions.assertNotNull(responseMessage.getContentData());
		Assertions.assertEquals(responseMessage.getContentData().length, responseMessage.getContentLength());

		RemotingResponseMessage responseMessage2 = new RemotingResponseMessage(requestId);
		responseMessage2.setContentTypeData(responseMessage.getContentTypeData());
		responseMessage2.setContentData(responseMessage.getContentData());

		responseMessage2.deserialize(DeserializeLevel.CONTENT_TYPE);
		Assertions.assertNotNull(responseMessage2.getContentType());
		Assertions.assertNull(responseMessage2.getContent());

		responseMessage2.deserialize(DeserializeLevel.HEADER);
		Assertions.assertNotNull(responseMessage2.getContentType());
		Assertions.assertNull(responseMessage2.getContent());

		responseMessage2.deserialize(DeserializeLevel.ALL);

		Assertions.assertEquals(responseMessage2.getContentType(), responseMessage.getContentType());
		Assertions.assertEquals(responseMessage2.getHeader(), responseMessage.getHeader());
		Assertions.assertTrue(responseMessage2.getContent() instanceof RemotingServerException);

	}

}
