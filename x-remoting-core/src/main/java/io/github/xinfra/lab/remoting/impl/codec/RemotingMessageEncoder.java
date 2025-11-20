package io.github.xinfra.lab.remoting.impl.codec;

import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemotingMessageEncoder implements MessageEncoder {

	@Override
	public void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
		try {
			if (msg instanceof RemotingMessage) {
				RemotingMessage remotingMessage = (RemotingMessage) msg;
				out.writeBytes(remotingMessage.protocolIdentifier().code());
				out.writeByte(remotingMessage.protocolIdentifier().version());
				out.writeByte(remotingMessage.messageType().data());
				out.writeInt(remotingMessage.id());
				out.writeByte(remotingMessage.serializationType().data());

				if (msg instanceof RemotingResponseMessage) {
					// write response status
					RemotingResponseMessage responseMessage = (RemotingResponseMessage) msg;
					out.writeShort(responseMessage.responseStatus().status());
				}
				int pathLength = 0;
				if (msg instanceof RemotingRequestMessage) {
					// write request path length
					RemotingRequestMessage requestMessage = (RemotingRequestMessage) msg;
					pathLength = requestMessage.getPathData().length;
					out.writeShort(pathLength);
				}

				// write header and body length
				int headerLength = remotingMessage.headers() == null ? 0 : remotingMessage.headers().data().length;
				int bodyLength = remotingMessage.body() == null ? 0 : remotingMessage.body().data().length;

				out.writeShort(headerLength);
				out.writeInt(bodyLength);

				// write request path
				if (pathLength > 0) {
					out.writeBytes(((RemotingRequestMessage) msg).getPathData());
				}

				// write header and body
				if (headerLength > 0) {
					out.writeBytes(remotingMessage.headers().data());
				}
				if (bodyLength > 0) {
					out.writeBytes(remotingMessage.body().data());
				}
			}
		}
		catch (Exception e) {
			log.error("RemotingMessageEncoder encode fail.", e);
			throw new CodecException("RemotingMessageEncoder encode fail.", e);
		}
	}

}
