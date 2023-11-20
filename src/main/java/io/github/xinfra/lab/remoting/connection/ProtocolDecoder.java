package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ProtocolDecoder extends ByteToMessageDecoder {
    private int protocolLength = 1;

    public ProtocolDecoder() {
    }

    public ProtocolDecoder(int protocolLength) {
        this.protocolLength = protocolLength;
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= protocolLength) {
            in.markReaderIndex();
            byte[] protocolCode = new byte[protocolLength];
            in.readBytes(protocolCode);
            in.resetReaderIndex();
            ProtocolType protocolType = ProtocolType.valueOf(protocolCode);
            ProtocolManager.getProtocol(protocolType).decoder().decode(ctx, in, out);
        }
    }
}
