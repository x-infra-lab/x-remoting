package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.RpcDeserializeLevel;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class RpcRequestMessageProcessor implements RemotingProcessor<RpcMessage> {
    private RpcMessageFactory rpcMessageFactory;
    private Map<String, UserProcessor<?>> userProcessors;

    public RpcRequestMessageProcessor(RpcMessageFactory rpcMessageFactory,
                                      Map<String, UserProcessor<?>> userProcessors) {
        this.rpcMessageFactory = rpcMessageFactory;
        this.userProcessors = userProcessors;
    }

    @Override
    public void handleMessage(RemotingContext remotingContext, RpcMessage message) throws Exception {
        RpcRequestMessage requestMessage = (RpcRequestMessage) message;

        if (!deserialize(requestMessage, RpcDeserializeLevel.content_type)) {
            return;
        }

        UserProcessor<?> userProcessor = userProcessors.get(message.getContentType());
        if (userProcessor == null) {
            log.error("No userProcessor for content-type:{}", message.getContentType());
            // TODO sendResponse

            return;
        }

        // TODO

    }

    private boolean deserialize(RpcMessage message, RpcDeserializeLevel level) {
        try {
            message.deserialize(level);
            return true;
        } catch (Throwable t) {
            log.error("Deserialize message fail. id:{} deserializeLevel:{}", message.id(), level, t);
            // TODO sendResponse
        }
        return false;
    }
}
