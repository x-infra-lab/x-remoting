package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.MessageTypeHandler;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.rpc.processor.MessageProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.UserProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.RpcHeartbeatMessageProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.RpcRequestMessageProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.RpcResponseMessageProcessor;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.xinfra.lab.remoting.rpc.message.RpcMessageType.heartbeatRequest;
import static io.github.xinfra.lab.remoting.rpc.message.RpcMessageType.request;

@Slf4j
public class RpcMessageHandler implements MessageHandler {

	private ConcurrentHashMap<RpcMessageType, MessageProcessor<RpcMessage>> messageProcessors = new ConcurrentHashMap<>();

	private ConcurrentHashMap<String, UserProcessor<?>> userProcessors = new ConcurrentHashMap<>();

	public RpcMessageHandler() {

		// request
		RpcRequestMessageProcessor rpcRequestMessageProcessor = new RpcRequestMessageProcessor();
		this.registerMessageProcessor(RpcMessageType.request, rpcRequestMessageProcessor);
		this.registerMessageProcessor(RpcMessageType.onewayRequest, rpcRequestMessageProcessor);
		// response
		this.registerMessageProcessor(RpcMessageType.response, new RpcResponseMessageProcessor());
		// heartbeat
		this.registerMessageProcessor(RpcMessageType.heartbeatRequest, new RpcHeartbeatMessageProcessor());
	}

	@Override
	public void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler) {

	}

	@Override
	public MessageTypeHandler messageTypeHandler(MessageType messageType) {
		return null;
	}

	@Override
	public void handleMessage(ChannelHandlerContext ctx, Message msg) {
		RpcMessage rpcMessage = (RpcMessage) msg;
		MessageHandlerContext messageHandlerContext = new MessageHandlerContext(ctx);
		try {
			rpcMessage.setRemoteAddress(messageHandlerContext.getConnection().remoteAddress());
			messageProcessors.get(rpcMessage.messageType()).handleMessage(messageHandlerContext, rpcMessage);
		}
		catch (Throwable t) {
			exceptionForMessage(messageHandlerContext, rpcMessage, t);
		}
	}

	private void registerMessageProcessor(RpcMessageType rpcMessageType, MessageProcessor<?> messageProcessor) {
		MessageProcessor<RpcMessage> prevMessageProcessor = messageProcessors.putIfAbsent(rpcMessageType,
				(MessageProcessor<RpcMessage>) messageProcessor);
		if (prevMessageProcessor != null) {
			throw new RuntimeException("repeat register message processor for " + rpcMessageType);
		}
	}

	public MessageProcessor<RpcMessage> messageProcessor(RpcMessageType rpcMessageType) {
		return messageProcessors.get(rpcMessageType);
	}

	public void registerUserProcessor(UserProcessor<?> userProcessor) {
		UserProcessor<?> prevUserProcessor = userProcessors.putIfAbsent(userProcessor.interest(), userProcessor);
		if (prevUserProcessor != null) {
			throw new RuntimeException("repeat register user processor for " + userProcessor.interest());
		}
	}

	public UserProcessor<?> userProcessor(String contentType) {
		return userProcessors.get(contentType);
	}

	private void exceptionForMessage(MessageHandlerContext messageHandlerContext, RpcMessage rpcMessage, Throwable t) {
		RpcMessageType rpcMessageType = rpcMessage.messageType();
		String errorMsg = String.format("handle %s message fail, id: %s", rpcMessageType, rpcMessage.id());
		log.error(errorMsg);
		if (request == rpcMessageType || heartbeatRequest == rpcMessageType) {
			RpcResponseMessage responseMessage = messageHandlerContext.getMessageFactory()
				.createExceptionResponse(rpcMessage.id(), t, errorMsg);
			RpcResponses.sendResponse(messageHandlerContext, responseMessage);
		}
	}

	@Override
	public void close() throws IOException {
		executor.shutdownNow();
		timer.stop();
	}

}
