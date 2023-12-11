package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;


import static io.github.xinfra.lab.remoting.connection.Connection.PROTOCOL;

@ChannelHandler.Sharable
public class ProtocolHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ProtocolType protocolType = ctx.channel().attr(PROTOCOL).get();
        Protocol protocol = ProtocolManager.getProtocol(protocolType);

        protocol.messageHandler().handleMessage(new RemotingContext(ctx), msg);

        ctx.fireChannelRead(msg);
    }
}
