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
				if (msg instanceof RemotingRequestMessage) {
					// write request path length
					RemotingRequestMessage requestMessage = (RemotingRequestMessage) msg;
					if (requestMessage.getPathData() != null) {
						out.writeShort(requestMessage.getPathData().length);
					}
					else {
						out.writeShort(0);
					}
				}

				// write header and body length
				if (remotingMessage.headers() != null) {
					out.writeShort(remotingMessage.headers().getHeaderData().length);
				}
				else {
					out.writeShort(0);
				}

				if (remotingMessage.body() != null) {
					out.writeInt(remotingMessage.body().getBodyData().length);
				}
				else {
					out.writeInt(0);
				}

				// write request path
				if (msg instanceof RemotingRequestMessage) {
					RemotingRequestMessage requestMessage = (RemotingRequestMessage) msg;
					if (requestMessage.getPathData() != null) {
						out.writeBytes(requestMessage.getPathData());
					}
				}

				// write header and body
				if (remotingMessage.headers() != null) {
					out.writeBytes(remotingMessage.headers().getHeaderData());
				}
				if (remotingMessage.body() != null) {
					out.writeBytes(remotingMessage.body().getBodyData());
				}
			}
		}
		catch (Exception e) {
			log.error("RemotingMessageEncoder encode fail.", e);
			throw new CodecException("RemotingMessageEncoder encode fail.", e);
		}
	}

}
