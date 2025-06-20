package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;

public class ProtocolDecoderTest {

	TestProtocol testProtocol = new TestProtocol();

	@Test
	public void testDecode() {
		ProtocolDecoder protocolDecoder = new ProtocolDecoder();
		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast(protocolDecoder);
		new Connection(testProtocol, channel);

		Message decodeMockMessage = mock(Message.class);
		MessageDecoder messageDecoder = new MessageDecoder() {
			@Override
			public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
				// simulate read all data
				in.readerIndex(in.readableBytes());
				// sumulate decode one message
				out.add(decodeMockMessage);
			}
		};
		testProtocol.setTestMessageDecoder(messageDecoder);

		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(testProtocol.protocolCode().code().length);
		byteBuf.writeBytes(testProtocol.protocolCode().code());
		channel.writeInbound(byteBuf);
		Assertions.assertTrue(channel.finish());
		Message message = (Message) channel.inboundMessages().poll();
		Assertions.assertEquals(message, decodeMockMessage);
		Assertions.assertEquals(byteBuf.refCnt(), 0);
	}

	@Test
	public void testDecodePartial() {
		ProtocolDecoder protocolDecoder = new ProtocolDecoder();
		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast(protocolDecoder);
		new Connection(testProtocol, channel);

		Message decodeMockMessage = mock(Message.class);
		MessageDecoder messageDecoder = new MessageDecoder() {
			@Override
			public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
				// simulate read all data
				in.readerIndex(in.readableBytes());
				// sumulate decode one message
				out.add(decodeMockMessage);
			}
		};
		testProtocol.setTestMessageDecoder(messageDecoder);

		// split data
		byte[] bytes = testProtocol.protocolCode().code();
		int length = bytes.length;

		int part1Length = length / 2;
		int part2Length = length - part1Length;

		byte[] part1 = new byte[part1Length];
		byte[] part2 = new byte[part2Length];
		System.arraycopy(bytes, 0, part1, 0, part1Length);
		System.arraycopy(bytes, part1Length, part2, 0, part2Length);

		// write partial data
		ByteBuf part1ByteBuf = ByteBufAllocator.DEFAULT.buffer(part1Length);
		part1ByteBuf.writeBytes(part1);
		channel.writeInbound(part1ByteBuf);
		Assertions.assertTrue(channel.inboundMessages().isEmpty());

		// write whole data
		ByteBuf part2ByteBuf = ByteBufAllocator.DEFAULT.buffer(part2Length);
		part2ByteBuf.writeBytes(part2);
		channel.writeInbound(part2ByteBuf);
		Assertions.assertTrue(channel.finish());

		Message message = (Message) channel.inboundMessages().poll();
		Assertions.assertEquals(message, decodeMockMessage);
		Assertions.assertEquals(part1ByteBuf.refCnt(), 0);
		Assertions.assertEquals(part2ByteBuf.refCnt(), 0);
	}

	@Test
	public void testDecodeException() {
		ProtocolDecoder protocolDecoder = new ProtocolDecoder();
		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast(protocolDecoder);
		new Connection(testProtocol, channel);

		ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(testProtocol.protocolCode().code().length);
		byteBuf.writeBytes(testProtocol.protocolCode().code());
		// Bad header
		ByteBuf invalidByteBuf = byteBuf.copy();
		invalidByteBuf.setByte(0, byteBuf.getByte(0) + 1);

		DecoderException decoderException = Assertions.assertThrows(DecoderException.class, () -> {
			channel.writeInbound(invalidByteBuf);
		});
		Assertions.assertTrue(decoderException.getCause() instanceof CodecException);

		byteBuf.release();
		Assertions.assertEquals(invalidByteBuf.refCnt(), 0);
	}

}
