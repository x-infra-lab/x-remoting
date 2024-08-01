package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.ConcurrentHashMap;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@ChannelHandler.Sharable
public class ProtocolHandler extends ChannelDuplexHandler {

    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors;

    public ProtocolHandler(ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        Validate.notNull(userProcessors, "userProcessors can not be null");
        this.userProcessors = userProcessors;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message) {
            Connection connection = ctx.channel().attr(CONNECTION).get();
            connection.getProtocol().messageHandler().handleMessage(new RemotingContext(ctx, userProcessors), msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
