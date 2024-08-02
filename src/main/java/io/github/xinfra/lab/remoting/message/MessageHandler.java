package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.processor.MessageProcessor;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.util.Timer;

import java.util.concurrent.ExecutorService;

public interface MessageHandler {

    ExecutorService executor();

    void handleMessage(MessageHandlerContext messageHandlerContext, Object msg);

    void registerMessageProcessor(MessageType messageType, MessageProcessor<?> messageProcessor);

    MessageProcessor<?> messageProcessor(MessageType messageType);

    void registerUserProcessor(UserProcessor<?> userProcessor);

    UserProcessor<?> userProcessor(String contentType);

    Timer timer();

}
