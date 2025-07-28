package io.github.xinfra.lab.remoting.impl.codec;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.exception.CodecException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.RemotingProtocolIdentifier;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RemotingMessageDecoder implements MessageDecoder {

    private int protocolCodeLength = RemotingProtocolIdentifier.PROTOCOL_CODE.length;

    private int minLength = Math.min(RemotingProtocol.RESPONSE_HEADER_BYTES, RemotingProtocol.REQUEST_HEADER_BYTES);

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= minLength) {
            in.markReaderIndex();
            in.skipBytes(protocolCodeLength);
            in.skipBytes(1); // skip protocol version

            byte messageTypeCode = in.readByte();
            MessageType messageType = MessageType.valueOf(messageTypeCode);

            int requestId = in.readInt();
            byte serializationTypeCode = in.readByte();
            SerializationType serializationType = SerializationManager.valueOf(serializationTypeCode);


            if (messageType == MessageType.response) {
                short status = in.readShort();
            }
            if (messageType == MessageType.heartbeat ||
                    messageType == MessageType.request ||
                    messageType == MessageType.oneway) {
                short pathDataLength = in.readShort();

            }

            short headerDataLength = in.readShort();
            int bodyDataLength = in.readInt();

            int remainLength = headerDataLength + bodyDataLength;

            if (remainLength <= in.readableBytes()) {
                RemotingMessage remotingMessage;
                switch (messageType) {
                    case MessageType.request:
                        remotingMessage = new RemotingRequestMessage(requestId, , serializationType);
                        break;
                    case onewayRequest:
                        remotingMessage = new RemotingRequestMessage(requestId, onewayRequest, serializationType);
                        break;
                    case response:
                        RemotingResponseMessage rpcResponseMessage = new RemotingResponseMessage(requestId, serializationType);
                        remotingMessage = rpcResponseMessage;
                        break;
                    case heartbeatRequest:
                        remotingMessage = new RpcHeartbeatRequestMessage(requestId, serializationType);
                        break;
                    default:
                        log.warn("MessageType not support:{}", rpcMessageType);
                        throw new CodecException("MessageType not support:" + rpcMessageType);

                }

                if (contentTypeLength > 0) {
                    byte[] bytes = new byte[contentTypeLength];
                    in.readBytes(bytes);
                    remotingMessage.setContentTypeData(bytes);
                }

                if (headerLength > 0) {
                    byte[] bytes = new byte[headerLength];
                    in.readBytes(bytes);
                    remotingMessage.setHeaderData(bytes);
                }
                if (contentLength > 0) {
                    byte[] bytes = new byte[contentLength];
                    in.readBytes(bytes);
                    remotingMessage.setContentData(bytes);
                }

                out.add(remotingMessage);
            } else {
                in.resetReaderIndex();
            }
        }
    }

}
