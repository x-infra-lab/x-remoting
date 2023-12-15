package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.message.RpcDeserializeLevel;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Executor;


@Slf4j
public class RpcRequestMessageProcessor implements RemotingProcessor<RpcMessage> {
    private RpcMessageFactory rpcMessageFactory;
    private Map<String, UserProcessor<?>> userProcessors;

    private Executor defaultExecutor;

    public RpcRequestMessageProcessor(RpcMessageFactory rpcMessageFactory,
                                      Executor defaultExecutor, Map<String, UserProcessor<?>> userProcessors) {
        this.rpcMessageFactory = rpcMessageFactory;
        this.defaultExecutor = defaultExecutor;
        this.userProcessors = userProcessors;
    }

    @Override
    public void handleMessage(RemotingContext remotingContext, RpcMessage message) throws Exception {
        RpcRequestMessage requestMessage = (RpcRequestMessage) message;

        if (!deserialize(remotingContext, requestMessage, RpcDeserializeLevel.content_type)) {
            return;
        }

        UserProcessor<?> userProcessor = userProcessors.get(message.getContentType());
        if (userProcessor == null) {
            String errorMsg = String.format("No userProcessor for content-type: %s", message.getContentType());
            log.error(errorMsg);
            sendResponse(remotingContext,
                    requestMessage,
                    rpcMessageFactory.createExceptionResponse(message.id(), errorMsg));
            return;
        }

        Executor executor;
        if (userProcessor.executorSelector() != null) {

            if (!deserialize(remotingContext, requestMessage, RpcDeserializeLevel.header)) {
                return;
            }

            executor = userProcessor.executorSelector()
                    .select(requestMessage.getContentType(), requestMessage.getHeader());
        } else {
            executor = userProcessor.executor();
        }

        if (executor == null) {
            executor = defaultExecutor;
        }

        executor.execute(new ProcessTask(userProcessor, requestMessage, remotingContext));
    }

    private void process(RemotingContext remotingContext,
                         UserProcessor userProcessor,
                         RpcRequestMessage requestMessage) {
        // TODO async


        ClassLoader originalClassLoader = null;
        try {

            if (userProcessor.getBizClassLoader() != null) {
                originalClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(userProcessor.getBizClassLoader());
            }

            Object responseContent = userProcessor.handRequest(requestMessage.getContent());
            sendResponse(remotingContext,
                    requestMessage,
                    rpcMessageFactory.createResponse(requestMessage.id(), responseContent)
            );
        } finally {
            if (originalClassLoader != null) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
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
