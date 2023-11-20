package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ProtocolType protocolType = ctx.channel().attr(Connection.PROTOCOL).get();
            if (protocolType != null) {
                ProtocolManager.getProtocol(protocolType)
                        .heartbeatTrigger()
                        .triggerHeartBeat(ctx.channel());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
