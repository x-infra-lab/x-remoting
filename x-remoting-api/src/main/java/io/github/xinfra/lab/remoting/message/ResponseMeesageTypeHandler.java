package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseMeesageTypeHandler implements MessageTypeHandler<ResponseMessage> {
    @Override
    public MessageType messageType() {
        return MessageType.response;
    }

    @Override
    public void handleMessage(Connection connection, ResponseMessage responseMessage) {
        int id = responseMessage.id();
        InvokeFuture<?> future = connection.removeInvokeFuture(id);
        if (future != null) {
            future.cancelTimeout();
            future.complete(responseMessage);
            try {
                future.executeCallBack();
            } catch (Throwable t) {
                log.error("executeCallBack fail. id:{}", responseMessage.id(), t);
                throw t;
            }
        } else {
            log.warn("can not find InvokeFuture maybe timeout. id:{} message:{} from:{}", responseMessage.id(),
                    responseMessage, connection.remoteAddress());
        }
    }
}
