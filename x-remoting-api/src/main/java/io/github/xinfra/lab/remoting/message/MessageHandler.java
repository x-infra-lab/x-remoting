package io.github.xinfra.lab.remoting.message;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Timer;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;

public interface MessageHandler extends Closeable {

	ExecutorService executor(Message message);

	void handleMessage(ChannelHandlerContext ctx, Message msg);

	Timer timer();

}
