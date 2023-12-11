package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;

public class RpcHeartbeatMessageProcessor implements RemotingProcessor<RpcMessage> {
    @Override
    public void handleMessage(RemotingContext remotingContext, RpcMessage message) {
        // TODO
    }
}
