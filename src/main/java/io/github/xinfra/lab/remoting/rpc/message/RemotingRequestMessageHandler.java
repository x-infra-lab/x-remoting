package io.github.xinfra.lab.remoting.rpc.message;

import org.apache.commons.lang3.Validate;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.rpc.exception.ResponseStatusRuntimeException;
import io.github.xinfra.lab.remoting.rpc.handler.RequestHandler;
import io.github.xinfra.lab.remoting.rpc.handler.ResponseObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
public class RemotingRequestMessageHandler {

	private final ConcurrentHashMap<String, RequestHandler> handlers = new ConcurrentHashMap<>();

	public <T, R> void register(String path, RequestHandler<T, R> requestHandler) {
		Validate.notBlank(path, "path can not be blank");
		RequestHandler prev = handlers.put(path, requestHandler);
		if (prev != null) {
			log.warn("RequestHandler for {} already registered, overwrite it: {}", path, prev);
		}
	}

	public RequestHandler lookup(String path) {
		return handlers.get(path);
	}

	public void handleMessage(Connection connection, RequestMessage requestMessage) {
		RemotingRequestMessage remotingRequestMessage = (RemotingRequestMessage) requestMessage;
		remotingRequestMessage.deserializePath();
		RequestHandler requestHandler = lookup(remotingRequestMessage.getPath());
		if (requestHandler == null) {
			log.warn("RequestHandler not found for path: {}", remotingRequestMessage.getPath());
			throw new ResponseStatusRuntimeException(ResponseStatus.NotFound);
		}
		Executor executor = requestHandler.getExecutor();
		if (executor == null) {
			executor = connection.getExecutor();
		}

		Runnable task = () -> {
			try {
				try {
					remotingRequestMessage.deserialize();
				}
				catch (DeserializeException e) {
					throw new ResponseStatusRuntimeException(ResponseStatus.DeserializeException, e);
				}
				ResponseObserver responseObserver = new ResponseObserver(connection, remotingRequestMessage);
				requestHandler.handle(remotingRequestMessage.getBody().getBodyValue(), responseObserver);
			}
			catch (Exception e) {
				log.error("request handler failed. id:{} path:{} remoteAddress:{}", remotingRequestMessage.getId(),
						remotingRequestMessage.getPath(), connection.remoteAddress(), e);
				Responses.handleExceptionResponse(connection, remotingRequestMessage, e);
			}
		};

		executor.execute(task);
	}

}
