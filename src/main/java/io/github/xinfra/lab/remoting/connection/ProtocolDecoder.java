package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.exception.CodecException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

public class ProtocolDecoder extends ByteToMessageDecoder {

	private static final Logger log = LoggerFactory.getLogger(ProtocolDecoder.class);

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			Connection connection = ctx.channel().attr(CONNECTION).get();
			byte[] protocolCode = connection.getProtocol().protocolCode();
			int protocolLength = protocolCode.length;

			if (in.readableBytes() >= protocolLength) {
				in.markReaderIndex();
				byte[] decodeProtocolCode = new byte[protocolLength];
				in.readBytes(decodeProtocolCode);
				in.resetReaderIndex();

				if (Arrays.equals(decodeProtocolCode, protocolCode)) {
					connection.getProtocol().decoder().decode(ctx, in, out);
				}
				else {
					throw new CodecException(
							"unknown protocol code:" + Arrays.toString(decodeProtocolCode) + " for protocolType");
				}
			}
		}
		catch (Exception e) {
			log.warn("ProtocolDecoder decode fail. ex:", e);
			in.skipBytes(in.readableBytes());
			throw e;
		}
	}

}
