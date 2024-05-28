package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.List;

import static io.github.xinfra.lab.remoting.connection.Connection.PROTOCOL;

public class ProtocolDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ProtocolType protocolType = ctx.channel().attr(PROTOCOL).get();
        Validate.notNull(protocolType, "ProtocolDecoder get protocolType is null");
        int protocolLength = protocolType.protocolCode().length;

        if (in.readableBytes() >= protocolLength) {
            in.markReaderIndex();
            byte[] protocolCode = new byte[protocolLength];
            in.readBytes(protocolCode);
            in.resetReaderIndex();

            if (Arrays.equals(protocolCode, protocolType.protocolCode())) {
                ProtocolManager.getProtocol(protocolType).decoder().decode(ctx, in, out);
            } else {
                throw new CodecException("unknown protocol code:" + Arrays.toString(protocolCode)
                        + " for protocolType:" + protocolType);
            }
        }
    }
}
