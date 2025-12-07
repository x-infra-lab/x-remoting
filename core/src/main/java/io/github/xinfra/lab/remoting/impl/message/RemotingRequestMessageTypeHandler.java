package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.ResponseStatusRuntimeException;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandler;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.impl.handler.ResponseObserver;
import io.github.xinfra.lab.remoting.message.AbstractRequestMessageTypeHandler;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemotingRequestMessageTypeHandler extends AbstractRequestMessageTypeHandler {

	private RequestHandlerRegistry requestHandlerRegistry;

	public RemotingRequestMessageTypeHandler(RequestHandlerRegistry requestHandlerRegistry) {
		this.requestHandlerRegistry = requestHandlerRegistry;
	}

	@Override
	public void handleMessage(Connection connection, RequestMessage requestMessage) {
		RemotingRequestMessage remotingRequestMessage = (RemotingRequestMessage) requestMessage;
		try {
			remotingRequestMessage.deserialize();
		}
		catch (DeserializeException e) {
			throw new ResponseStatusRuntimeException(ResponseStatus.DeserializeException, e);
		}
		ResponseObserver responseObserver = new ResponseObserver(connection, remotingRequestMessage);
		RequestHandler requestHandler = requestHandlerRegistry.lookup(remotingRequestMessage.getPath());
		if (requestHandler == null) {
			log.warn("RequestHandler not found for path: {}", remotingRequestMessage.getPath());
			throw new ResponseStatusRuntimeException(ResponseStatus.NotFound);
		}
		requestHandler.asyncHandle(remotingRequestMessage.getBody().getBodyValue(), responseObserver);
	}

}
