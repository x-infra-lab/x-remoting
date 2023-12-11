package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;
import io.github.xinfra.lab.remoting.processor.UserProcessor;

import java.util.Map;

public class RpcRequestMessageProcessor implements RemotingProcessor<RpcMessage> {
    private RpcMessageFactory rpcMessageFactory;
    private  Map<String, UserProcessor<?>> userProcessors;

    public RpcRequestMessageProcessor(RpcMessageFactory rpcMessageFactory,
                                      Map<String, UserProcessor<?>> userProcessors) {
        this.rpcMessageFactory = rpcMessageFactory;
        this.userProcessors = userProcessors;
    }

    @Override
    public void handleMessage(RemotingContext remotingContext, RpcMessage message) {
        // TODO
    }
}
