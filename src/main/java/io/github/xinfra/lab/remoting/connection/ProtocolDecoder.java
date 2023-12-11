package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.github.xinfra.lab.remoting.connection.Connection.PROTOCOL;

public class ProtocolDecoder extends ByteToMessageDecoder {


    public ProtocolDecoder() {
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Set<ProtocolType> protocolTypes = ProtocolManager.getProtocolTypes();
        for (ProtocolType protocolType : protocolTypes) {
            int protocolLength = protocolType.protocolCode().length;

            if (in.readableBytes() >= protocolLength) {
                in.markReaderIndex();
                byte[] protocolCode = new byte[protocolLength];
                in.readBytes(protocolCode);
                in.resetReaderIndex();

                if (Arrays.equals(protocolCode, protocolType.protocolCode())) {
                    ctx.channel().attr(PROTOCOL).set(protocolType);
                    ProtocolManager.getProtocol(protocolType).decoder().decode(ctx, in, out);
                }
            }

        }
    }
}
