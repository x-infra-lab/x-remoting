package io.github.xinfra.lab.remoting;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

public class RemotingContext {

    @Getter
    private ChannelHandlerContext channelContext;

    public RemotingContext(ChannelHandlerContext ctx) {
        this.channelContext = ctx;
    }

}
