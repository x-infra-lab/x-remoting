package io.github.xinfra.lab.remoting.impl.codec;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.impl.message.ResponseStatus;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageHeaders;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.xinfra.lab.remoting.impl.RemotingProtocol.RESPONSE_HEADER_BYTES;
import static org.mockito.Mockito.mock;

public class RpcMessageDecoderTest {

	@Test
	public void testDecodeRequest1() throws Exception {
		// build a requestMessage data
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

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), requestMessage, byteBuf);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);
		Assertions.assertTrue(!out.isEmpty());
		RemotingRequestMessage requestMessage2 = (RemotingRequestMessage) out.get(0);
		requestMessage2.deserialize();

		Assertions.assertEquals(requestMessage2.serializationType(), requestMessage.serializationType());

		Assertions.assertEquals(requestMessage2.getContentType(), requestMessage.getContentType());
		Assertions.assertEquals(requestMessage2.getHeader(), requestMessage.getHeader());
		Assertions.assertEquals(requestMessage2.getContent(), requestMessage.getContent());

		byteBuf.release();

	}

	@Test
	public void testDecodeResponse1() throws Exception {
		// build a responseMessage data
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

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);
		Assertions.assertTrue(!out.isEmpty());

		RemotingResponseMessage responseMessage2 = (RemotingResponseMessage) out.get(0);
		responseMessage2.deserialize();

		Assertions.assertEquals(responseMessage2.serializationType(), responseMessage2.serializationType());
		Assertions.assertEquals(responseMessage2.getStatus(), responseMessage.getStatus());
		Assertions.assertEquals(responseMessage2.getContentType(), responseMessage.getContentType());
		Assertions.assertEquals(responseMessage2.getHeader(), responseMessage.getHeader());
		Assertions.assertEquals(responseMessage2.getContent(), responseMessage.getContent());

		byteBuf.release();
	}

	@Test
	public void testDecodeRequestFailed1() throws Exception {
		// build a requestMessage data
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

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), requestMessage, byteBuf);

		// half of data
		int mid = byteBuf.readableBytes() / 2;
		int readerIndex = byteBuf.readerIndex();
		byteBuf.setIndex(readerIndex, readerIndex + mid);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);

		Assertions.assertTrue(out.isEmpty());
		Assertions.assertEquals(readerIndex, byteBuf.readerIndex());

		byteBuf.release();
	}

	@Test
	public void testDecodeResponseFailed1() throws Exception {
		// build a responseMessage data
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

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

		// half of data
		int mid = byteBuf.readableBytes() / 2;
		int readerIndex = byteBuf.readerIndex();
		byteBuf.setIndex(readerIndex, readerIndex + mid);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);

		Assertions.assertTrue(out.isEmpty());
		Assertions.assertEquals(readerIndex, byteBuf.readerIndex());

		byteBuf.release();
	}

	@Test
	public void testDecodeResponseFailed2() throws Exception {
		// build a responseMessage data
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

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

		// less than response data header length
		int readerIndex = byteBuf.readerIndex();
		byteBuf.setIndex(readerIndex, RESPONSE_HEADER_BYTES - 1);

		RemotingMessageDecoder decoder = new RemotingMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);

		Assertions.assertTrue(out.isEmpty());
		Assertions.assertEquals(readerIndex, byteBuf.readerIndex());

		byteBuf.release();
	}

}
