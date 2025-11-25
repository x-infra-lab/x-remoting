package io.github.xinfra.lab.remoting.impl.codec;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageBody;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.message.DefaultMessageHeaders;
import io.github.xinfra.lab.remoting.message.MessageHeaders;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage.RESPONSE_HEADER_BYTES;
import static org.mockito.Mockito.mock;

public class RemotingMessageDecoderTest {

	@Test
	public void testDecodeRequest1() throws Exception {
		// build a requestMessage getData
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

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = Unpooled.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), requestMessage, byteBuf);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);
		Assertions.assertTrue(!out.isEmpty());
		RemotingRequestMessage decodedRequestMessage = (RemotingRequestMessage) out.get(0);
		decodedRequestMessage.deserialize();

		Assertions.assertEquals(decodedRequestMessage.getMessageType(), requestMessage.getMessageType());

		Assertions.assertEquals(decodedRequestMessage.getId(), requestMessage.getId());
		Assertions.assertEquals(decodedRequestMessage.getSerializationType(), requestMessage.getSerializationType());
		Assertions.assertEquals(decodedRequestMessage.getPath(), requestMessage.getPath());

		Assertions.assertEquals(decodedRequestMessage.getHeaders().get(headerKey), headerValue);
		Assertions.assertEquals(decodedRequestMessage.getBody().getBodyValue(), content);

		byteBuf.release();

	}

	@Test
	public void testDecodeResponse1() throws Exception {
		// build a responseMessage getData
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

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = Unpooled.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);
		Assertions.assertTrue(!out.isEmpty());

		RemotingResponseMessage decodedResponseMessage = (RemotingResponseMessage) out.get(0);
		decodedResponseMessage.deserialize();

		Assertions.assertEquals(decodedResponseMessage.getMessageType(), responseMessage.getMessageType());
		Assertions.assertEquals(decodedResponseMessage.getId(), responseMessage.getId());
		Assertions.assertEquals(decodedResponseMessage.getSerializationType(),
				decodedResponseMessage.getSerializationType());
		Assertions.assertEquals(decodedResponseMessage.getResponseStatus(), responseMessage.getResponseStatus());
		Assertions.assertEquals(decodedResponseMessage.getHeaders().get(headerKey), headerValue);
		Assertions.assertEquals(decodedResponseMessage.getBody().getBodyValue(), content);
		byteBuf.release();
	}

	@Test
	public void testDecodeRequestFailed1() throws Exception {
		// build a requestMessage getData
		String content = "this is rpc content";
		DefaultMessageHeaders header = new DefaultMessageHeaders();
		header.put(MessageHeaders.Key.stringKey("test-key"), "test-value");
		Integer requestId = IDGenerator.nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hession);
		requestMessage.setPath("/test");
		requestMessage.setHeaders(header);
		requestMessage.setBody(new RemotingMessageBody(content));
		requestMessage.serialize();

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = Unpooled.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), requestMessage, byteBuf);

		// half of getData
		int mid = byteBuf.readableBytes() / 2;
		int readerIndex = byteBuf.readerIndex();
		byteBuf.setIndex(readerIndex, readerIndex + mid);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);

		// assert not decode
		Assertions.assertTrue(out.isEmpty());
		// assert readerIndex not change
		Assertions.assertEquals(readerIndex, byteBuf.readerIndex());

		byteBuf.release();
	}

	@Test
	public void testDecodeResponseFailed1() throws Exception {
		// build a responseMessage getData
		String content = "this is rpc content";
		DefaultMessageHeaders header = new DefaultMessageHeaders();
		header.put(MessageHeaders.Key.stringKey("test-key"), "test-value");
		Integer requestId = IDGenerator.nextRequestId();
		RemotingResponseMessage responseMessage = new RemotingResponseMessage(requestId, SerializationType.Hession,
				ResponseStatus.OK);
		responseMessage.setHeaders(header);
		responseMessage.setBody(new RemotingMessageBody(content));
		responseMessage.serialize();

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = Unpooled.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

		// half of getData
		int mid = byteBuf.readableBytes() / 2;
		int readerIndex = byteBuf.readerIndex();
		byteBuf.setIndex(readerIndex, readerIndex + mid);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);

		// assert not decode
		Assertions.assertTrue(out.isEmpty());
		// assert readerIndex not change
		Assertions.assertEquals(readerIndex, byteBuf.readerIndex());

		byteBuf.release();
	}

	@Test
	public void testDecodeResponseFailed2() throws Exception {
		// build a responseMessage getData
		String content = "this is rpc content";
		DefaultMessageHeaders header = new DefaultMessageHeaders();
		header.put(MessageHeaders.Key.stringKey("test-key"), "test-value");
		Integer requestId = IDGenerator.nextRequestId();
		RemotingResponseMessage responseMessage = new RemotingResponseMessage(requestId, SerializationType.Hession,
				ResponseStatus.OK);
		responseMessage.setHeaders(header);
		responseMessage.setBody(new RemotingMessageBody(content));
		responseMessage.serialize();

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = Unpooled.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

		// less than response getData header length
		int readerIndex = byteBuf.readerIndex();
		byteBuf.setIndex(readerIndex, RESPONSE_HEADER_BYTES - 1);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);

		// assert not decode
		Assertions.assertTrue(out.isEmpty());
		// assert readerIndex not change
		Assertions.assertEquals(readerIndex, byteBuf.readerIndex());

		byteBuf.release();
	}

}
