package io.github.xinfra.lab.remoting.rpc.codec;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.rpc.protocol.RemotingProtocolId;
import io.github.xinfra.lab.remoting.rpc.message.RemotingMessage;
import io.github.xinfra.lab.remoting.rpc.message.RemotingMessageBody;
import io.github.xinfra.lab.remoting.rpc.message.DefaultMessageHeaders;
import io.github.xinfra.lab.remoting.rpc.message.MessageType;
import io.github.xinfra.lab.remoting.rpc.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static io.github.xinfra.lab.remoting.rpc.message.MessageType.*;

@Slf4j
public class RemotingMessageDecoder implements MessageDecoder {

	private int protocolCodeLength = RemotingProtocolId.PROTOCOL_CODE.length;

	private int minLength = Math.min(RemotingRequestMessage.REQUEST_HEADER_BYTES,
			RemotingResponseMessage.RESPONSE_HEADER_BYTES);

	@Override
	public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			if (in.readableBytes() >= minLength) {
				in.markReaderIndex();
				in.skipBytes(protocolCodeLength);
				in.skipBytes(1); // skip getProtocol version

				byte messageTypeCode = in.readByte();
				MessageType messageType = valueOf(messageTypeCode);

				int requestId = in.readInt();
				byte serializationTypeCode = in.readByte();
				SerializationType serializationType = SerializationManager.valueOf(serializationTypeCode);

				ResponseStatus responseStatus = null;
				if (messageType == response || messageType == goaway) {
					short status = in.readShort();
					responseStatus = ResponseStatus.valueOf(status);
				}

				int pathDataLength = 0;
				if (messageType == request || messageType == heartbeatRequest) {
					pathDataLength = in.readUnsignedShort();
				}

				int headerDataLength = in.readUnsignedShort();
				int bodyDataLength = in.readInt();
				if (bodyDataLength < 0) {
					throw new CodecException("negative body length: " + bodyDataLength);
				}

				long remainLength = (long) pathDataLength + headerDataLength + bodyDataLength;

				if (remainLength <= in.readableBytes()) {
					RemotingMessage remotingMessage;

					if (messageType == request || messageType == heartbeatRequest) {
						RemotingRequestMessage remotingRequestMessage = new RemotingRequestMessage(requestId,
								messageType, serializationType);
						if (pathDataLength > 0) {
							byte[] bytes = new byte[pathDataLength];
							in.readBytes(bytes);
							remotingRequestMessage.setPathData(bytes);
						}
						remotingMessage = remotingRequestMessage;
					}
					else if (messageType == response || messageType == goaway) {
						remotingMessage = new RemotingResponseMessage(requestId, messageType, serializationType,
								responseStatus);
					}
					else {
						log.warn("MessageType not support:{} remoteAddress:{}", messageType,
								ctx.channel().remoteAddress());
						throw new CodecException("MessageType not support:" + messageType);
					}

					if (headerDataLength > 0) {
						byte[] bytes = new byte[headerDataLength];
						in.readBytes(bytes);
						remotingMessage.setHeaders(new DefaultMessageHeaders(bytes));
					}
					if (bodyDataLength > 0) {
						byte[] bytes = new byte[bodyDataLength];
						in.readBytes(bytes);
						remotingMessage.setBody(new RemotingMessageBody(bytes));
					}

					out.add(remotingMessage);
				}
				else {
					in.resetReaderIndex();
				}
			}
		}
		catch (Exception e) {
			Object remoteAddress = null;
			try {
				remoteAddress = ctx.channel().remoteAddress();
			}
			catch (Exception ignored) {
			}
			log.error("RemotingMessageDecoder decode fail. remoteAddress:{}", remoteAddress, e);
			throw new CodecException("RemotingMessageDecoder decode fail.", e);
		}
	}

}
