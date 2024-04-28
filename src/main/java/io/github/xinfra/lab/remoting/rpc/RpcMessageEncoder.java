package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcMessageEncoder implements MessageEncoder {
    @Override
    public void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
        if (msg instanceof RpcMessage) {
            RpcMessage rpcMessage = (RpcMessage) msg;
            out.writeBytes(msg.protocolType().protocolCode());
            out.writeByte(msg.messageType().data());
            out.writeInt(msg.id());
            out.writeByte(msg.serializationType().data());
            if (msg instanceof RpcResponseMessage) {
                RpcResponseMessage rpcResponseMessage = (RpcResponseMessage) msg;
                out.writeShort(rpcResponseMessage.getStatus());
            }

            out.writeShort(rpcMessage.getContentTypeLength());
            out.writeShort(rpcMessage.getHeaderLength());
            out.writeInt(rpcMessage.getContentLength());

            if (rpcMessage.getContentTypeLength() > 0) {
                out.writeBytes(rpcMessage.getContentTypeData());
            }
            if (rpcMessage.getHeaderLength() > 0) {
                out.writeBytes(rpcMessage.getHeaderData());
            }
            if (rpcMessage.getContentLength() > 0) {
                out.writeBytes(rpcMessage.getContentData());
            }
            
        } else {
            log.warn("Message type not support:{}", msg.getClass());
        }
    }
}
