package io.github.xinfra.lab.remoting.processor;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.Message;

public interface RemotingProcessor<T extends Message> {
    void handleMessage(RemotingContext remotingContext, T message);
}
