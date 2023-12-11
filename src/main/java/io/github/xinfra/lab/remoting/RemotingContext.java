package io.github.xinfra.lab.remoting;

import io.netty.channel.ChannelHandlerContext;

public class RemotingContext {
    private ChannelHandlerContext ctx;

    public RemotingContext(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }
}
