package io.github.xinfra.lab.remoting;

import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

public class RemotingContext {

    @Getter
    private ChannelHandlerContext channelContext;

    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors;


    public RemotingContext(ChannelHandlerContext ctx, ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        this.channelContext = ctx;
        this.userProcessors = userProcessors;
    }

    public UserProcessor<?> getUserProcessor(String contentType) {
        return userProcessors.get(contentType);
    }
}
