package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ProtocolEncoder extends MessageToByteEncoder<Message> {


    public ProtocolEncoder() {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        Connection connection = ctx.channel().attr(Connection.CONNECTION).get();
        connection.getProtocol().encoder().encode(ctx, msg, out);
    }
}
