package io.github.xinfra.lab.remoting.rpc.codec;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageHeader;
import io.github.xinfra.lab.remoting.rpc.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.xinfra.lab.remoting.rpc.RpcProtocol.RESPONSE_HEADER_LEN;
import static org.mockito.Mockito.mock;

public class RpcMessageDecoderTest {

	@Test
	public void testDecodeRequest1() throws Exception {
		// build a requestMessage data
		String content = "this is rpc content";
		String contentType = content.getClass().getName();
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcRequestMessage requestMessage = new RpcRequestMessage(requestId);
		requestMessage.setHeader(header);
		requestMessage.setContent(content);
		requestMessage.setContentType(contentType);
		requestMessage.serialize();

		RpcMessageEncoder encoder = new RpcMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), requestMessage, byteBuf);

		RpcMessageDecoder decoder = new RpcMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);
		Assertions.assertTrue(!out.isEmpty());
		RpcRequestMessage requestMessage2 = (RpcRequestMessage) out.get(0);
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
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcResponseMessage responseMessage = new RpcResponseMessage(requestId);
		responseMessage.setHeader(header);
		responseMessage.setContent(content);
		responseMessage.setContentType(contentType);
		responseMessage.setStatus(ResponseStatus.SUCCESS.getCode());
		responseMessage.serialize();

		RpcMessageEncoder encoder = new RpcMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

		RpcMessageDecoder decoder = new RpcMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);
		Assertions.assertTrue(!out.isEmpty());

		RpcResponseMessage responseMessage2 = (RpcResponseMessage) out.get(0);
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
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcRequestMessage requestMessage = new RpcRequestMessage(requestId);
		requestMessage.setHeader(header);
		requestMessage.setContent(content);
		requestMessage.setContentType(contentType);
		requestMessage.serialize();

		RpcMessageEncoder encoder = new RpcMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), requestMessage, byteBuf);

		// half of data
		int mid = byteBuf.readableBytes() / 2;
		int readerIndex = byteBuf.readerIndex();
		byteBuf.setIndex(readerIndex, readerIndex + mid);

		RpcMessageDecoder decoder = new RpcMessageDecoder();
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
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcResponseMessage responseMessage = new RpcResponseMessage(requestId);
		responseMessage.setHeader(header);
		responseMessage.setContent(content);
		responseMessage.setContentType(contentType);
		responseMessage.setStatus(ResponseStatus.SUCCESS.getCode());
		responseMessage.serialize();

		RpcMessageEncoder encoder = new RpcMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

		// half of data
		int mid = byteBuf.readableBytes() / 2;
		int readerIndex = byteBuf.readerIndex();
		byteBuf.setIndex(readerIndex, readerIndex + mid);

		RpcMessageDecoder decoder = new RpcMessageDecoder();
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
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcResponseMessage responseMessage = new RpcResponseMessage(requestId);
		responseMessage.setHeader(header);
		responseMessage.setContent(content);
		responseMessage.setContentType(contentType);
		responseMessage.setStatus(ResponseStatus.SUCCESS.getCode());
		responseMessage.serialize();

		RpcMessageEncoder encoder = new RpcMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

		// less than response data header length
		int readerIndex = byteBuf.readerIndex();
		byteBuf.setIndex(readerIndex, RESPONSE_HEADER_LEN - 1);

		RpcMessageDecoder decoder = new RpcMessageDecoder();
		List<Object> out = new ArrayList<>();
		decoder.decode(mock(ChannelHandlerContext.class), byteBuf, out);

		Assertions.assertTrue(out.isEmpty());
		Assertions.assertEquals(readerIndex, byteBuf.readerIndex());

		byteBuf.release();
	}

}
