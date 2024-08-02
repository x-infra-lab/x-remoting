package io.github.xinfra.lab.remoting.heartbeat;

import io.netty.channel.ChannelHandlerContext;

public interface HeartbeatTrigger {

    void triggerHeartBeat(ChannelHandlerContext ctx);

    void setHeartbeatMaxFailCount(int failCount);

    void setHeartbeatTimeoutMills(int timeoutMills);
}
