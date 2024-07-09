package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;

public class ProtocolEncoderTest {

    public static final ProtocolType TEST_ENCODER = new ProtocolType("x-unit-test".getBytes(StandardCharsets.UTF_8));

    @BeforeClass
    public static void beforeClass() {
        ProtocolManager.registerProtocolIfAbsent(TEST_ENCODER, new TestProtocol());
    }

    @Test
    public void testEncode() {
        ProtocolEncoder protocolEncoder = new ProtocolEncoder();
        Message message = mock(Message.class);

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(protocolEncoder);
        channel.attr(Connection.PROTOCOL).set(TEST_ENCODER);

        byte[] data = "testEncode".getBytes(StandardCharsets.UTF_8);

        MessageEncoder messageEncoder = new MessageEncoder() {
            @Override
            public void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
                Assert.assertEquals(message, msg);
                out.writeBytes(data);
            }
        };


        Protocol protocol = ProtocolManager.getProtocol(TEST_ENCODER);
        ((TestProtocol) protocol).setTestMessageEncoder(messageEncoder);

        boolean written = channel.writeOutbound(message);
        Assert.assertTrue(written);
        Assert.assertTrue(!channel.outboundMessages().isEmpty());
        ByteBuf byteBuf = (ByteBuf) channel.outboundMessages().poll();

        int readableBytes = byteBuf.readableBytes();
        byte[] bytes = new byte[readableBytes];
        byteBuf.readBytes(bytes, 0, readableBytes);

        Assert.assertArrayEquals(data, bytes);
    }
}
