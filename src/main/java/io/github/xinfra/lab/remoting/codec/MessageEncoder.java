package io.github.xinfra.lab.remoting.codec;

import io.github.xinfra.lab.remoting.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface MessageEncoder {

    void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception;

}
