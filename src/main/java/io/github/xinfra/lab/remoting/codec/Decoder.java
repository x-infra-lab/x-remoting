package io.github.xinfra.lab.remoting.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public interface Decoder {
    void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out);
}
