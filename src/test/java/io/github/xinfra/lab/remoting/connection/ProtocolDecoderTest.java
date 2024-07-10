package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;


public class ProtocolDecoderTest {

    @Test
    public void testDecoder1() {
        ProtocolType testProtocol = new ProtocolType("testDecoder1".getBytes());
        ProtocolManager.registerProtocolIfAbsent(testProtocol, new TestProtocol());
        ProtocolDecoder protocolDecoder = new ProtocolDecoder();

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(protocolDecoder);
        channel.attr(Connection.PROTOCOL).set(testProtocol);

        Message decodeMockMessage = mock(Message.class);
        MessageDecoder messageDecoder = new MessageDecoder() {
            @Override
            public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
                // simulate read all data
                in.readBytes(in.readableBytes());
                // sumulate decode one message
                out.add(decodeMockMessage);
            }
        };
        Protocol protocol = ProtocolManager.getProtocol(testProtocol);
        ((TestProtocol) protocol).setTestMessageDecoder(messageDecoder);

        ByteBuf byteBuf = Unpooled.wrappedBuffer(testProtocol.protocolCode());
        boolean written = channel.writeInbound(byteBuf);
        Assert.assertTrue(written);
        Assert.assertTrue(!channel.inboundMessages().isEmpty());
        Message message = (Message) channel.inboundMessages().poll();
        Assert.assertEquals(message, decodeMockMessage);
    }

    @Test
    public void testDecoder2() {
        ProtocolType testProtocol = new ProtocolType("testDecoder2".getBytes());
        ProtocolManager.registerProtocolIfAbsent(testProtocol, new TestProtocol());
        ProtocolDecoder protocolDecoder = new ProtocolDecoder();

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(protocolDecoder);
        channel.attr(Connection.PROTOCOL).set(testProtocol);

        Message decodeMockMessage = mock(Message.class);
        MessageDecoder messageDecoder = new MessageDecoder() {
            @Override
            public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
                // simulate read all data
                in.readBytes(in.readableBytes());
                // sumulate decode one message
                out.add(decodeMockMessage);
            }
        };
        Protocol protocol = ProtocolManager.getProtocol(testProtocol);
        ((TestProtocol) protocol).setTestMessageDecoder(messageDecoder);

        // split data
        byte[] bytes = testProtocol.protocolCode();
        int length = bytes.length;


        int part1Length = length / 2;
        int part2Length = length - part1Length;

        byte[] part1 = new byte[part1Length];
        byte[] part2 = new byte[part2Length];
        System.arraycopy(bytes, 0, part1, 0, part1Length);
        System.arraycopy(bytes, part1Length, part2, 0, part2Length);

        // write partial data
        ByteBuf part1ByteBuf = Unpooled.wrappedBuffer(part1);
        boolean written = channel.writeInbound(part1ByteBuf);
        Assert.assertFalse(written);
        Assert.assertTrue(channel.inboundMessages().isEmpty());

        // write whole data
        ByteBuf part2ByteBuf = Unpooled.wrappedBuffer(part2);
        written = channel.writeInbound(part2ByteBuf);
        Assert.assertTrue(written);
        Assert.assertTrue(!channel.inboundMessages().isEmpty());
        Message message = (Message) channel.inboundMessages().poll();
        Assert.assertEquals(message, decodeMockMessage);
    }
}
