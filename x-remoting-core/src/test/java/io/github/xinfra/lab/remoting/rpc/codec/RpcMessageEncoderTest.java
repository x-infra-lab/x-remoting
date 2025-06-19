package io.github.xinfra.lab.remoting.rpc.codec;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageType;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageHeader;
import io.github.xinfra.lab.remoting.rpc.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import io.netty.buffer.AbstractByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class RpcMessageEncoderTest {

	@Test
	public void testEncodeRequest1() throws Exception {
		// build a requestMessage
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

		Assertions.assertTrue(byteBuf.readableBytes() > 0);

		byte[] protocolCodes = RpcProtocol.PROTOCOL_CODE;
		byte[] dataProtocolCodes = new byte[protocolCodes.length];
		byteBuf.readBytes(dataProtocolCodes);
		Assertions.assertArrayEquals(protocolCodes, dataProtocolCodes);
		Assertions.assertEquals(byteBuf.readByte(), RpcMessageType.request.data());
		Assertions.assertEquals(byteBuf.readInt(), requestId);
		Assertions.assertEquals(byteBuf.readByte(), requestMessage.serializationType().data());
		Assertions.assertEquals(byteBuf.readShort(), requestMessage.getContentTypeLength());
		Assertions.assertEquals(byteBuf.readShort(), requestMessage.getHeaderLength());
		Assertions.assertEquals(byteBuf.readInt(), requestMessage.getContentLength());

		byte[] dataContentType = new byte[requestMessage.getContentTypeLength()];
		byteBuf.readBytes(dataContentType);
		Assertions.assertArrayEquals(requestMessage.getContentTypeData(), dataContentType);

		byte[] dataHeader = new byte[requestMessage.getHeaderLength()];
		byteBuf.readBytes(dataHeader);
		Assertions.assertArrayEquals(requestMessage.getHeaderData(), dataHeader);

		byte[] dataContent = new byte[requestMessage.getContentLength()];
		byteBuf.readBytes(dataContent);
		Assertions.assertArrayEquals(requestMessage.getContentData(), dataContent);

		byteBuf.release();
	}

	@Test
	public void testEncodeResponse2() throws Exception {
		// build a responseMessage
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

		Assertions.assertTrue(byteBuf.readableBytes() > 0);

		byte[] protocolCodes = RpcProtocol.PROTOCOL_CODE;
		byte[] dataProtocolCodes = new byte[protocolCodes.length];
		byteBuf.readBytes(dataProtocolCodes);
		Assertions.assertArrayEquals(protocolCodes, dataProtocolCodes);
		Assertions.assertEquals(byteBuf.readByte(), RpcMessageType.response.data());
		Assertions.assertEquals(byteBuf.readInt(), requestId);
		Assertions.assertEquals(byteBuf.readByte(), responseMessage.serializationType().data());
		Assertions.assertEquals(byteBuf.readShort(), responseMessage.getStatus());
		Assertions.assertEquals(byteBuf.readShort(), responseMessage.getContentTypeLength());
		Assertions.assertEquals(byteBuf.readShort(), responseMessage.getHeaderLength());
		Assertions.assertEquals(byteBuf.readInt(), responseMessage.getContentLength());

		byte[] dataContentType = new byte[responseMessage.getContentTypeLength()];
		byteBuf.readBytes(dataContentType);
		Assertions.assertArrayEquals(responseMessage.getContentTypeData(), dataContentType);

		byte[] dataHeader = new byte[responseMessage.getHeaderLength()];
		byteBuf.readBytes(dataHeader);
		Assertions.assertArrayEquals(responseMessage.getHeaderData(), dataHeader);

		byte[] dataContent = new byte[responseMessage.getContentLength()];
		byteBuf.readBytes(dataContent);
		Assertions.assertArrayEquals(responseMessage.getContentData(), dataContent);

		byteBuf.release();
	}

	@Test
	public void testEncodeException1() throws Exception {

		RpcMessageEncoder encoder = new RpcMessageEncoder();
		ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();

		Assertions.assertThrows(CodecException.class, () -> {
			encoder.encode(mock(ChannelHandlerContext.class), mock(Message.class), byteBuf);
		});

		byteBuf.release();
	}

}
