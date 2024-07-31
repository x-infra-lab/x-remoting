package io.github.xinfra.lab.remoting.rpc.codec;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.message.Message;
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
    public void testEncode1() throws Exception {

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

        RpcMessageEncoder encoder = new RpcMessageEncoder();
        ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
        encoder.encode(mock(ChannelHandlerContext.class), requestMessage, byteBuf);

        Assertions.assertTrue(byteBuf.readableBytes() > 0);
    }

    @Test
    public void testEncode2() throws Exception {

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

        RpcMessageEncoder encoder = new RpcMessageEncoder();
        ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();
        encoder.encode(mock(ChannelHandlerContext.class), responseMessage, byteBuf);

        Assertions.assertTrue(byteBuf.readableBytes() > 0);
    }

    @Test
    public void testEncode3() throws Exception {


        RpcMessageEncoder encoder = new RpcMessageEncoder();
        ByteBuf byteBuf = AbstractByteBufAllocator.DEFAULT.buffer();

        Assertions.assertThrows(CodecException.class, () -> {
            encoder.encode(mock(ChannelHandlerContext.class), mock(Message.class), byteBuf);
        });
    }
}
