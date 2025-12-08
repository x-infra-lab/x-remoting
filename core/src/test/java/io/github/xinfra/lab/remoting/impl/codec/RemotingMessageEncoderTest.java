package io.github.xinfra.lab.remoting.impl.codec;

import io.github.xinfra.lab.remoting.common.ArraysUtils;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocolId;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageBody;
import io.github.xinfra.lab.remoting.message.DefaultMessageHeaders;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.message.MessageHeaders;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class RemotingMessageEncoderTest {

	@Test
	public void testEncodeRequest1() throws Exception {
		// build a requestMessage
		String content = "this is rpc content";
		DefaultMessageHeaders header = new DefaultMessageHeaders();
		header.put(MessageHeaders.Key.stringKey("test-key"), "test-value");
		Integer requestId = IDGenerator.nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hession);
		requestMessage.setHeaders(header);
		requestMessage.setPath("/test-encode");
		requestMessage.setBody(new RemotingMessageBody(content));
		requestMessage.serialize();

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = Unpooled.buffer();
		encoder.encode(mock(ChannelHandlerContext.class), requestMessage, byteBuf);

		Assertions.assertTrue(byteBuf.readableBytes() > 0);

		// assert getProtocol getCodes
		byte[] protocolCodes = RemotingProtocolId.INSTANCE.getCodes();
		byte[] dataProtocolCodes = new byte[protocolCodes.length];
		byteBuf.readBytes(dataProtocolCodes);
		Assertions.assertArrayEquals(protocolCodes, dataProtocolCodes);
		// assert getProtocol version
		Assertions.assertEquals(byteBuf.readByte(), RemotingProtocolId.INSTANCE.version());
		// assert message type
		Assertions.assertEquals(byteBuf.readByte(), MessageType.request.getCode());
		// assert requestId
		Assertions.assertEquals(byteBuf.readInt(), requestId);
		// assert serialization type
		Assertions.assertEquals(byteBuf.readByte(), requestMessage.getSerializationType().getCode());

		int pathDataLength = requestMessage.getPathData().length;
		int headerDataLength = requestMessage.getHeaders().getDataTotalLength();
		int bodyDataLength = requestMessage.getBody().getDataTotalLength();

		Assertions.assertEquals(byteBuf.readShort(), pathDataLength);
		Assertions.assertEquals(byteBuf.readShort(), headerDataLength);
		Assertions.assertEquals(byteBuf.readInt(), bodyDataLength);

		byte[] pathData = new byte[pathDataLength];
		byteBuf.readBytes(pathData);
		Assertions.assertArrayEquals(requestMessage.getPathData(), pathData);

		byte[] headerData = new byte[headerDataLength];
		byteBuf.readBytes(headerData);
		byte[] headerBytes = ArraysUtils.concat(requestMessage.getHeaders().getData());
		Assertions.assertArrayEquals(headerBytes, headerData);

		byte[] bodyData = new byte[bodyDataLength];
		byteBuf.readBytes(bodyData);
		byte[] bodyBytes = ArraysUtils.concat(requestMessage.getBody().getData());
		Assertions.assertArrayEquals(bodyBytes, bodyData);

		Assertions.assertEquals(byteBuf.readableBytes(), 0);

		byteBuf.release();
	}

	@Test
	public void testEncodeResponse2() throws Exception {
		// build a responseMessage
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

		Assertions.assertTrue(byteBuf.readableBytes() > 0);

		// assert getProtocol getCodes
		byte[] protocolCodes = RemotingProtocolId.INSTANCE.getCodes();
		byte[] dataProtocolCodes = new byte[protocolCodes.length];
		byteBuf.readBytes(dataProtocolCodes);
		Assertions.assertArrayEquals(protocolCodes, dataProtocolCodes);
		// assert getProtocol version
		Assertions.assertEquals(byteBuf.readByte(), RemotingProtocolId.INSTANCE.version());
		// assert message type
		Assertions.assertEquals(byteBuf.readByte(), MessageType.response.getCode());
		// assert requestId
		Assertions.assertEquals(byteBuf.readInt(), requestId);
		// assert serialization type
		Assertions.assertEquals(byteBuf.readByte(), responseMessage.getSerializationType().getCode());
		// assert response status
		Assertions.assertEquals(byteBuf.readShort(), responseMessage.getResponseStatus().status());

		int headerDataLength = responseMessage.getHeaders().getDataTotalLength();
		int bodyDataLength = responseMessage.getBody().getDataTotalLength();

		Assertions.assertEquals(byteBuf.readShort(), headerDataLength);
		Assertions.assertEquals(byteBuf.readInt(), bodyDataLength);

		byte[] headerData = new byte[headerDataLength];
		byteBuf.readBytes(headerData);

		byte[] headerBytes = ArraysUtils.concat(responseMessage.getHeaders().getData());
		Assertions.assertArrayEquals(headerBytes, headerData);

		byte[] bodyData = new byte[bodyDataLength];
		byteBuf.readBytes(bodyData);

		byte[] bodyBytes = ArraysUtils.concat(responseMessage.getBody().getData());
		Assertions.assertArrayEquals(bodyBytes, bodyData);

		Assertions.assertEquals(byteBuf.readableBytes(), 0);

		byteBuf.release();
	}

	@Test
	public void testEncodeException1() throws Exception {

		RemotingMessageEncoder encoder = new RemotingMessageEncoder();
		ByteBuf byteBuf = Unpooled.buffer();

		Assertions.assertThrows(CodecException.class, () -> {
			encoder.encode(mock(ChannelHandlerContext.class), mock(RemotingMessage.class), byteBuf);
		});

		byteBuf.release();
	}

}
