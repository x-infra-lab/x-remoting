package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;
import io.github.xinfra.lab.remoting.processor.UserProcessor;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RpcMessageHandler implements MessageHandler {

    private Map<MessageType, RemotingProcessor<RpcMessage>> remotingProcessors;

    private Map<String, UserProcessor<?>> userProcessors;

    private RpcMessageFactory rpcMessageFactory;

    // TODO: use config
    private Executor defaultExecutor = new ThreadPoolExecutor(20, 400, 60,
            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024),
            new NamedThreadFactory("RPC-MESSAGE-HANDLER-")
    );

    public RpcMessageHandler(RpcMessageFactory rpcMessageFactory) {
        this.rpcMessageFactory = rpcMessageFactory;

        remotingProcessors.put(MessageType.request, new RpcRequestMessageProcessor(rpcMessageFactory, userProcessors));
        remotingProcessors.put(MessageType.response, new RpcResponseMessageProcessor());

        RpcHeartbeatMessageProcessor rpcHeartbeatMessageProcessor = new RpcHeartbeatMessageProcessor();
        remotingProcessors.put(MessageType.heartbeatRequest, rpcHeartbeatMessageProcessor);
        remotingProcessors.put(MessageType.heartbeatResponse, rpcHeartbeatMessageProcessor);
    }

    @Override
    public Executor executor() {
        return defaultExecutor;
    }

    @Override
    public void handleMessage(RemotingContext remotingContext, Object msg) {
        RpcMessage rpcMessage = (RpcMessage) msg;
        try {
            remotingProcessors.get(rpcMessage.messageType())
                    .handleMessage(remotingContext, rpcMessage);
        } catch (Throwable t) {
            // todo
        }
    }

    @Override
    public void registerUserProcessor(UserProcessor<?> userProcessor) {

    }
}
