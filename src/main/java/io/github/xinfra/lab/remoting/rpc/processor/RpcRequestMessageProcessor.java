package io.github.xinfra.lab.remoting.rpc.processor;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.rpc.message.RpcDeserializeLevel;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponses;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;


@Slf4j
public class RpcRequestMessageProcessor implements RemotingProcessor<RpcMessage> {
    private RpcMessageFactory rpcMessageFactory;

    private Executor executor;

    public RpcRequestMessageProcessor(RpcMessageFactory rpcMessageFactory,
                                      Executor executor) {
        this.rpcMessageFactory = rpcMessageFactory;
        this.executor = executor;
    }

    @Override
    public void handleMessage(RemotingContext remotingContext, RpcMessage message) throws Exception {
        RpcRequestMessage requestMessage = (RpcRequestMessage) message;

        if (!deserialize(remotingContext, requestMessage, RpcDeserializeLevel.CONTENT_TYPE)) {
            return;
        }

        UserProcessor<?> userProcessor = remotingContext.getUserProcessor(message.getContentType());
        if (userProcessor == null) {
            String errorMsg = String.format("No userProcessor for content-type: %s", message.getContentType());
            log.error(errorMsg);
            sendResponse(remotingContext,
                    requestMessage,
                    rpcMessageFactory.createExceptionResponse(message.id(), errorMsg));
            return;
        }

        Executor userProcessorExecutor;
        if (userProcessor.executorSelector() != null) {

            if (!deserialize(remotingContext, requestMessage, RpcDeserializeLevel.HEADER)) {
                return;
            }

            userProcessorExecutor = userProcessor.executorSelector()
                    .select(requestMessage.getContentType(), requestMessage.getHeader());
        } else {
            userProcessorExecutor = userProcessor.executor();
        }

        if (userProcessorExecutor == null) {
            userProcessorExecutor = this.executor;
        }

        userProcessorExecutor.execute(new ProcessTask(userProcessor, requestMessage, remotingContext));
    }

    private void process(RemotingContext remotingContext,
                         UserProcessor userProcessor,
                         RpcRequestMessage requestMessage) {

        if (!deserialize(remotingContext, requestMessage, RpcDeserializeLevel.ALL)) {
            return;
        }

        // TODO async


        ClassLoader contextClassLoader = null;
        try {
            if (userProcessor.getBizClassLoader() != null) {
                contextClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(userProcessor.getBizClassLoader());
            }

            Object responseContent = userProcessor.handRequest(requestMessage.getContent());
            sendResponse(remotingContext,
                    requestMessage,
                    rpcMessageFactory.createResponse(requestMessage.id(), responseContent)
            );
        } finally {
            if (contextClassLoader != null) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }

    }

    private void sendResponse(RemotingContext remotingContext,
                              RpcRequestMessage requestMessage,
                              RpcResponseMessage responseMessage) {
        if (requestMessage.messageType() != MessageType.onewayRequest) {
            RpcResponses.sendResponse(remotingContext, responseMessage, rpcMessageFactory);
        }
    }

    private boolean deserialize(RemotingContext remotingContext, RpcRequestMessage requestMessage, RpcDeserializeLevel level) {
        try {
            requestMessage.deserialize(level);
            return true;
        } catch (Throwable t) {
            log.error("Deserialize message fail. id:{} deserializeLevel:{}", requestMessage.id(), level, t);

            RpcResponseMessage responseMessage = rpcMessageFactory.createExceptionResponse(requestMessage.id(), t,
                    ResponseStatus.SERVER_DESERIAL_EXCEPTION);

            sendResponse(remotingContext, requestMessage, responseMessage);
        }
        return false;
    }


    class ProcessTask implements Runnable {

        private UserProcessor<?> userProcessor;
        private RpcRequestMessage requestMessage;
        private RemotingContext remotingContext;


        public ProcessTask(UserProcessor<?> userProcessor,
                           RpcRequestMessage requestMessage,
                           RemotingContext remotingContext) {
            this.userProcessor = userProcessor;
            this.requestMessage = requestMessage;
            this.remotingContext = remotingContext;
        }

        @Override
        public void run() {
            try {
                process(remotingContext, userProcessor, requestMessage);
            } catch (Throwable t) {
                int id = requestMessage.id();
                String errorMsg = String.format("user process message fail. id: %s", id);
                log.error(errorMsg, t);
                sendResponse(remotingContext, requestMessage,
                        rpcMessageFactory.createExceptionResponse(id, t, errorMsg));
            }
        }
    }


}
