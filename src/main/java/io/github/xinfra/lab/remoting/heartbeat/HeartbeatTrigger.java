package io.github.xinfra.lab.remoting.heartbeat;

import io.netty.channel.ChannelHandlerContext;

public interface HeartbeatTrigger {
    void triggerHeartBeat(ChannelHandlerContext ctx);
}
