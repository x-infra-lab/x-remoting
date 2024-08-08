package io.github.xinfra.lab.remoting.rpc.processor;

import io.github.xinfra.lab.remoting.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.processor.AbstractMessageProcessor;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.rpc.message.RpcDeserializeLevel;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponses;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

@Slf4j
public class RpcRequestMessageProcessor extends AbstractMessageProcessor<RpcMessage> {

	@Override
	public void handleMessage(MessageHandlerContext messageHandlerContext, RpcMessage message) throws Exception {
		RpcRequestMessage requestMessage = (RpcRequestMessage) message;
		if (!deserialize(messageHandlerContext, requestMessage, RpcDeserializeLevel.CONTENT_TYPE)) {
			return;
		}

		UserProcessor<?> userProcessor = messageHandlerContext.getUserProcessor(message.getContentType());
		if (userProcessor == null) {
			String errorMsg = String.format("No userProcessor for content-type: %s", message.getContentType());
			log.error(errorMsg);
			sendResponse(messageHandlerContext, requestMessage,
					messageHandlerContext.getMessageFactory().createExceptionResponse(message.id(), errorMsg));
			return;
		}

		// use UserProcessor define executor
		Executor processorExecutor;
		if (userProcessor.executorSelector() != null) {
			if (!deserialize(messageHandlerContext, requestMessage, RpcDeserializeLevel.HEADER)) {
				return;
			}

			processorExecutor = userProcessor.executorSelector()
				.select(requestMessage.getContentType(), requestMessage.getHeader());
		}
		else {
			processorExecutor = userProcessor.executor();
		}

		if (processorExecutor == null) {
			// use MessageProcessor define executor
			processorExecutor = executor();
			if (processorExecutor == null) {
				// use MessageHandler define default executor
				processorExecutor = messageHandlerContext.getMessageDefaultExecutor();
			}
		}

		processorExecutor.execute(new ProcessTask(userProcessor, requestMessage, messageHandlerContext));
	}

	private void process(MessageHandlerContext messageHandlerContext, UserProcessor userProcessor,
			RpcRequestMessage requestMessage) {

		if (!deserialize(messageHandlerContext, requestMessage, RpcDeserializeLevel.ALL)) {
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
			sendResponse(messageHandlerContext, requestMessage,
					messageHandlerContext.getMessageFactory().createResponse(requestMessage.id(), responseContent));
		}
		finally {
			if (contextClassLoader != null) {
				Thread.currentThread().setContextClassLoader(contextClassLoader);
			}
		}

	}

	private void sendResponse(MessageHandlerContext messageHandlerContext, RpcRequestMessage requestMessage,
			RpcResponseMessage responseMessage) {
		if (requestMessage.messageType() != MessageType.onewayRequest) {
			RpcResponses.sendResponse(messageHandlerContext, responseMessage);
		}
	}

	private boolean deserialize(MessageHandlerContext messageHandlerContext, RpcRequestMessage requestMessage,
			RpcDeserializeLevel level) {
		try {
			requestMessage.deserialize(level);
			return true;
		}
		catch (Throwable t) {
			log.error("Deserialize message fail. id:{} deserializeLevel:{}", requestMessage.id(), level, t);

			if (!(t instanceof DeserializeException)) {
				t = new DeserializeException("Deserialize requestMessage fail.", t);
			}
			RpcResponseMessage responseMessage = messageHandlerContext.getMessageFactory()
				.createExceptionResponse(requestMessage.id(), t, ResponseStatus.SERVER_DESERIAL_EXCEPTION);

			sendResponse(messageHandlerContext, requestMessage, responseMessage);
		}
		return false;
	}

	class ProcessTask implements Runnable {

		private UserProcessor<?> userProcessor;

		private RpcRequestMessage requestMessage;

		private MessageHandlerContext messageHandlerContext;

		public ProcessTask(UserProcessor<?> userProcessor, RpcRequestMessage requestMessage,
				MessageHandlerContext messageHandlerContext) {
			this.userProcessor = userProcessor;
			this.requestMessage = requestMessage;
			this.messageHandlerContext = messageHandlerContext;
		}

		@Override
		public void run() {
			try {
				process(messageHandlerContext, userProcessor, requestMessage);
			}
			catch (Throwable t) {
				int id = requestMessage.id();
				String errorMsg = String.format("user process message fail. id: %s", id);
				log.error(errorMsg, t);
				sendResponse(messageHandlerContext, requestMessage,
						messageHandlerContext.getMessageFactory().createExceptionResponse(id, t, errorMsg));
			}
		}

	}

}
