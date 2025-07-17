package io.github.xinfra.lab.remoting.rpc.codec;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageType;
import io.github.xinfra.lab.remoting.rpc.message.RpcHeartbeatRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static io.github.xinfra.lab.remoting.rpc.message.RpcMessageType.onewayRequest;

@Slf4j
public class RpcMessageDecoder implements MessageDecoder {

	private int protocolCodeLength = RpcProtocol.PROTOCOL_CODE.length;

	private int minLength = Math.min(RpcProtocol.RESPONSE_HEADER_LEN, RpcProtocol.REQUEST_HEADER_LEN);

	@Override
	public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (in.readableBytes() >= minLength) {
			in.markReaderIndex();
			in.skipBytes(protocolCodeLength);

			byte messageTypeCode = in.readByte();
			RpcMessageType rpcMessageType = RpcMessageType.valueOf(messageTypeCode);

			// check for response
			int alreadyRead = protocolCodeLength + 1;
			if (rpcMessageType == RpcMessageType.response
					&& in.readableBytes() < RpcProtocol.RESPONSE_HEADER_LEN - alreadyRead) {
				in.resetReaderIndex();
				return;
			}

			int requestId = in.readInt();
			byte serializationTypeCode = in.readByte();
			SerializationType serializationType = SerializationManager.valueOf(serializationTypeCode);

			short status = 0;
			if (rpcMessageType == RpcMessageType.response) {
				status = in.readShort();
			}

			short contentTypeLength = in.readShort();
			short headerLength = in.readShort();
			int contentLength = in.readInt();

			int remainLength = contentTypeLength + headerLength + contentLength;

			if (remainLength <= in.readableBytes()) {
				RpcMessage rpcMessage;
				switch (rpcMessageType) {
					case request:
						rpcMessage = new RpcRequestMessage(requestId, serializationType);
						break;
					case onewayRequest:
						rpcMessage = new RpcRequestMessage(requestId, onewayRequest, serializationType);
						break;
					case response:
						RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(requestId, serializationType);
						rpcResponseMessage.setStatus(status);
						rpcMessage = rpcResponseMessage;
						break;
					case heartbeatRequest:
						rpcMessage = new RpcHeartbeatRequestMessage(requestId, serializationType);
						break;
					default:
						log.warn("MessageType not support:{}", rpcMessageType);
						throw new CodecException("MessageType not support:" + rpcMessageType);

				}

				if (contentTypeLength > 0) {
					byte[] bytes = new byte[contentTypeLength];
					in.readBytes(bytes);
					rpcMessage.setContentTypeData(bytes);
				}

				if (headerLength > 0) {
					byte[] bytes = new byte[headerLength];
					in.readBytes(bytes);
					rpcMessage.setHeaderData(bytes);
				}
				if (contentLength > 0) {
					byte[] bytes = new byte[contentLength];
					in.readBytes(bytes);
					rpcMessage.setContentData(bytes);
				}

				out.add(rpcMessage);
			}
			else {
				in.resetReaderIndex();
			}
		}
	}

}
