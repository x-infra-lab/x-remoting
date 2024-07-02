package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.commons.lang3.Validate;

public class ProtocolEncoder extends MessageToByteEncoder<Message> {


    public ProtocolEncoder() {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        ProtocolType protocolType = ctx.channel().attr(Connection.PROTOCOL).get();
        Validate.notNull(protocolType, "ProtocolEncoder get protocolType is null");
        ProtocolManager.getProtocol(protocolType).encoder().encode(ctx, msg, out);
    }
}
