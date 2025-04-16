package io.github.xinfra.lab.remoting.rpc.processor;

import io.github.xinfra.lab.remoting.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.message.Message;

import java.util.concurrent.ExecutorService;

public interface MessageProcessor<T extends Message> {

	void handleMessage(MessageHandlerContext messageHandlerContext, T message) throws Exception;

	void executor(ExecutorService executor);

	ExecutorService executor();

}
