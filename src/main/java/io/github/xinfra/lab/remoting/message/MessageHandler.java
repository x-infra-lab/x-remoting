package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.processor.UserProcessor;

import java.util.concurrent.Executor;

public interface MessageHandler {

    Executor executor();


    void handleMessage(RemotingContext remotingContext, Object msg);

    void registerUserProcessor(UserProcessor<?> userProcessor);
}
