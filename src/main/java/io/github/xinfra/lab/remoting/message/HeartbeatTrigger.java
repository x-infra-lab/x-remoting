package io.github.xinfra.lab.remoting.message;

import io.netty.channel.ChannelHandlerContext;

public interface HeartbeatTrigger {
    void triggerHeartBeat(ChannelHandlerContext ctx);
}
