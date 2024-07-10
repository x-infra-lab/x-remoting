package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.EncoderException;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;

public class ProtocolEncoderTest {

    @Test
    public void testEncode() {
        ProtocolType testProtocol = new ProtocolType("testEncode".getBytes());
        ProtocolManager.registerProtocolIfAbsent(testProtocol, new TestProtocol());

        ProtocolEncoder protocolEncoder = new ProtocolEncoder();
        Message message = mock(Message.class);

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(protocolEncoder);
        channel.attr(Connection.PROTOCOL).set(testProtocol);

        byte[] data = "testEncode".getBytes(StandardCharsets.UTF_8);

        MessageEncoder messageEncoder = new MessageEncoder() {
            @Override
            public void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
                Assert.assertEquals(message, msg);
                out.writeBytes(data);
            }
        };


        Protocol protocol = ProtocolManager.getProtocol(testProtocol);
        ((TestProtocol) protocol).setTestMessageEncoder(messageEncoder);

        boolean written = channel.writeOutbound(message);
        Assert.assertTrue(written);
        Assert.assertTrue(!channel.outboundMessages().isEmpty());
        ByteBuf byteBuf = (ByteBuf) channel.outboundMessages().poll();
        Assert.assertNotEquals(Unpooled.EMPTY_BUFFER, byteBuf);

        int readableBytes = byteBuf.readableBytes();
        byte[] bytes = new byte[readableBytes];
        byteBuf.readBytes(bytes, 0, readableBytes);

        Assert.assertArrayEquals(data, bytes);
    }


    @Test
    public void testEncodeException() {
        ProtocolType testProtocol = new ProtocolType("testEncodeException".getBytes());
        ProtocolManager.registerProtocolIfAbsent(testProtocol, new TestProtocol());

        ProtocolEncoder protocolEncoder = new ProtocolEncoder();
        Message message = mock(Message.class);

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(protocolEncoder);
        channel.attr(Connection.PROTOCOL).set(testProtocol);


        MessageEncoder messageEncoder = new MessageEncoder() {
            @Override
            public void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
                throw new CodecException("test exception");
            }
        };


        Protocol protocol = ProtocolManager.getProtocol(testProtocol);
        ((TestProtocol) protocol).setTestMessageEncoder(messageEncoder);

        EncoderException encoderException = Assert.assertThrows(EncoderException.class, () -> {
            channel.writeOutbound(message);
        });

        Assert.assertTrue(encoderException.getCause() instanceof CodecException);

    }
}
