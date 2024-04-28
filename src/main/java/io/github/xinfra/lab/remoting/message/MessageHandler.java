package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.RemotingContext;

import java.util.concurrent.Executor;

public interface MessageHandler {

    Executor executor();

    void handleMessage(RemotingContext remotingContext, Object msg);

}
