package io.github.xinfra.lab.remoting.message;

import io.netty.channel.Channel;

public interface HeartbeatTrigger {
    void triggerHeartBeat(Channel channel);
}
