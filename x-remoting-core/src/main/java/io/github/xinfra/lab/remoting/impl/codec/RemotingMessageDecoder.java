package io.github.xinfra.lab.remoting.impl.codec;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.RemotingProtocolIdentifier;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageBody;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageHeaders;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static io.github.xinfra.lab.remoting.message.MessageType.*;

@Slf4j
public class RemotingMessageDecoder implements MessageDecoder {

	private int protocolCodeLength = RemotingProtocolIdentifier.PROTOCOL_CODE.length;

	private int minLength = Math.min(RemotingProtocol.RESPONSE_HEADER_BYTES, RemotingProtocol.REQUEST_HEADER_BYTES);

	@Override
	public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			if (in.readableBytes() >= minLength) {
				in.markReaderIndex();
				in.skipBytes(protocolCodeLength);
				in.skipBytes(1); // skip protocol version

				byte messageTypeCode = in.readByte();
				MessageType messageType = valueOf(messageTypeCode);

				int requestId = in.readInt();
				byte serializationTypeCode = in.readByte();
				SerializationType serializationType = SerializationManager.valueOf(serializationTypeCode);

				ResponseStatus responseStatus = null;
				if (messageType == response) {
					short status = in.readShort();
					responseStatus = ResponseStatus.valueOf(status);
				}

				short pathDataLength = 0;
				if (messageType == heartbeat || messageType == request || messageType == oneway) {
					pathDataLength = in.readShort();
				}

				short headerDataLength = in.readShort();
				int bodyDataLength = in.readInt();

				int remainLength = pathDataLength + headerDataLength + bodyDataLength;

				if (remainLength <= in.readableBytes()) {
					RemotingMessage remotingMessage;

					if (messageType == heartbeat || messageType == request || messageType == oneway) {
						RemotingRequestMessage remotingRequestMessage = new RemotingRequestMessage(requestId,
								messageType, serializationType);

						if (pathDataLength > 0) {
							byte[] bytes = new byte[pathDataLength];
							in.readBytes(bytes);
							remotingRequestMessage.setPathData(bytes);
						}
						remotingMessage = remotingRequestMessage;
					}
					else if (messageType == response) {
						remotingMessage = new RemotingResponseMessage(requestId, messageType, serializationType,
								responseStatus);
					}
					else {
						log.warn("MessageType not support:{}", messageType);
						throw new CodecException("MessageType not support:" + messageType);
					}

					if (headerDataLength > 0) {
						byte[] bytes = new byte[headerDataLength];
						in.readBytes(bytes);
						remotingMessage.setHeaders(new RemotingMessageHeaders(bytes));
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
			log.error("RemotingMessageDecoder encode fail.", e);
			throw new CodecException("RemotingMessageDecoder decode fail.", e);
		}
	}

}
