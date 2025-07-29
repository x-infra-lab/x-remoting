package io.github.xinfra.lab.remoting.impl.processor;

import io.github.xinfra.lab.remoting.impl.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.impl.message.RpcMessageType;
import io.github.xinfra.lab.remoting.impl.message.ResponseStatus;
import io.github.xinfra.lab.remoting.impl.message.DeserializeLevel;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponses;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

@Slf4j
public class RpcRequestMessageProcessor extends AbstractMessageProcessor<RemotingMessage> {

	@Override
	public void handleMessage(MessageHandlerContext messageHandlerContext, RemotingMessage message) throws Exception {
		RemotingRequestMessage requestMessage = (RemotingRequestMessage) message;
		if (!deserialize(messageHandlerContext, requestMessage, DeserializeLevel.CONTENT_TYPE)) {
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
			if (!deserialize(messageHandlerContext, requestMessage, DeserializeLevel.HEADER)) {
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
			RemotingRequestMessage requestMessage) {

		if (!deserialize(messageHandlerContext, requestMessage, DeserializeLevel.ALL)) {
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

	private void sendResponse(MessageHandlerContext messageHandlerContext, RemotingRequestMessage requestMessage,
			RemotingResponseMessage responseMessage) {
		if (requestMessage.messageType() != RpcMessageType.onewayRequest) {
			RemotingResponses.sendResponse(messageHandlerContext, responseMessage);
		}
	}

	private boolean deserialize(MessageHandlerContext messageHandlerContext, RemotingRequestMessage requestMessage,
			DeserializeLevel level) {
		try {
			requestMessage.deserialize(level);
			return true;
		}
		catch (Throwable t) {
			log.error("Deserialize message fail. id:{} deserializeLevel:{}", requestMessage.id(), level, t);

			if (!(t instanceof DeserializeException)) {
				t = new DeserializeException("Deserialize requestMessage fail.", t);
			}
			RemotingResponseMessage responseMessage = messageHandlerContext.getMessageFactory()
				.createExceptionResponse(requestMessage.id(), t, ResponseStatus.SERVER_DESERIAL_EXCEPTION);

			sendResponse(messageHandlerContext, requestMessage, responseMessage);
		}
		return false;
	}

	class ProcessTask implements Runnable {

		private UserProcessor<?> userProcessor;

		private RemotingRequestMessage requestMessage;

		private MessageHandlerContext messageHandlerContext;

		public ProcessTask(UserProcessor<?> userProcessor, RemotingRequestMessage requestMessage,
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
