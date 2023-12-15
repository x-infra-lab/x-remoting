package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;

public class RpcHeartbeatMessageProcessor implements RemotingProcessor<RpcMessage> {
    private RpcMessageFactory rpcMessageFactory;

    public RpcHeartbeatMessageProcessor(RpcMessageFactory rpcMessageFactory) {
        this.rpcMessageFactory = rpcMessageFactory;
    }

    @Override
    public void handleMessage(RemotingContext remotingContext, RpcMessage message) {
        RpcResponses.sendResponse(remotingContext,
                rpcMessageFactory.createResponse(message.id(), null),
                rpcMessageFactory);
    }
}
