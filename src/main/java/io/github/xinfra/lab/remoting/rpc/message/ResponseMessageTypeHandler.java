package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.rpc.client.InFlightRequests;
import io.github.xinfra.lab.remoting.rpc.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseMessageTypeHandler implements MessageTypeHandler<ResponseMessage> {

	@Override
	public MessageType getMessageType() {
		return MessageType.response;
	}

	@Override
	public void handleMessage(Connection connection, ResponseMessage responseMessage) {
		int id = responseMessage.getId();
		InvokeFuture<?> future = InFlightRequests.of(connection).remove(id);
		if (future != null) {
			future.cancelTimeout();
			future.complete(responseMessage);
			try {
				future.executeCallBack(connection.getExecutor());
			}
			catch (Throwable t) {
				log.error("executeCallBack fail. getId:{}", responseMessage.getId(), t);
			}
		}
		else {
			log.warn("can not find InvokeFuture maybe timeout. getId:{} message:{} from:{}", responseMessage.getId(),
					responseMessage, connection.remoteAddress());
		}
	}

}
