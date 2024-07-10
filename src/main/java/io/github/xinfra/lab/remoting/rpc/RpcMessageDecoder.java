package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.RpcHeartbeatRequestMessage;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static io.github.xinfra.lab.remoting.message.MessageType.onewayRequest;


@Slf4j
public class RpcMessageDecoder implements MessageDecoder {
    private int protocolCodeLength = RpcProtocol.RPC.protocolCode().length;

    private int minLength = Math.min(RpcProtocol.RESPONSE_HEADER_LEN, RpcProtocol.REQUEST_HEADER_LEN);

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= minLength) {
            in.markReaderIndex();
            in.skipBytes(protocolCodeLength);

            byte messageTypeCode = in.readByte();
            MessageType messageType = MessageType.valueOf(messageTypeCode);

            // check for response
            int alreadyRead = protocolCodeLength + 1;
            if (messageType == MessageType.response && in.readableBytes() < RpcProtocol.RESPONSE_HEADER_LEN - alreadyRead) {
                in.resetReaderIndex();
                return;
            }

            int requestId = in.readInt();
            byte serializationTypeCode = in.readByte();
            SerializationType serializationType = SerializationType.valueOf(serializationTypeCode);

            short status = 0;
            if (messageType == MessageType.response) {
                status = in.readShort();
            }

            short contentTypeLength = in.readShort();
            short headerLength = in.readShort();
            int contentLength = in.readInt();

            int remainLength = contentTypeLength + headerLength + contentLength;

            if (remainLength <= in.readableBytes()) {
                RpcMessage rpcMessage;
                switch (messageType) {
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
                        log.warn("MessageType not support:{}", messageType);
                        throw new CodecException("MessageType not support:" + messageType);

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
            } else {
                in.resetReaderIndex();
            }
        }
    }

}
