package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.message.HeartbeatMessageTypeHandler;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.MessageTypeHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;



@Slf4j
public class RemotingMessageHandler implements MessageHandler {

	private ConcurrentHashMap<MessageType, MessageTypeHandler<RemotingMessage>>
			messageTypeHandlers = new ConcurrentHashMap<>();


	public RemotingMessageHandler() {
		// heartbeat
		this.registerMessageTypeHandler(new HeartbeatMessageTypeHandler());
//		// request
//		RpcRequestMessageProcessor rpcRequestMessageProcessor = new RpcRequestMessageProcessor();
//		this.registerMessageProcessor(RpcMessageType.request, rpcRequestMessageProcessor);
//		this.registerMessageProcessor(RpcMessageType.onewayRequest, rpcRequestMessageProcessor);
//		// response
//		this.registerMessageProcessor(RpcMessageType.response, new RpcResponseMessageProcessor());

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
//		RemotingMessage remotingMessage = (RemotingMessage) msg;
//		MessageHandlerContext messageHandlerContext = new MessageHandlerContext(ctx);
//		try {
//			remotingMessage.setRemoteAddress(messageHandlerContext.getConnection().remoteAddress());
//			messageProcessors.get(remotingMessage.messageType()).handleMessage(messageHandlerContext, remotingMessage);
//		}
//		catch (Throwable t) {
//			exceptionForMessage(messageHandlerContext, remotingMessage, t);
//		}
	}
//
//	private void registerMessageProcessor(RpcMessageType rpcMessageType, MessageProcessor<?> messageProcessor) {
//		MessageProcessor<RemotingMessage> prevMessageProcessor = messageProcessors.putIfAbsent(rpcMessageType,
//				(MessageProcessor<RemotingMessage>) messageProcessor);
//		if (prevMessageProcessor != null) {
//			throw new RuntimeException("repeat register message processor for " + rpcMessageType);
//		}
//	}
//
//	public MessageProcessor<RemotingMessage> messageProcessor(RpcMessageType rpcMessageType) {
//		return messageProcessors.get(rpcMessageType);
//	}
//
//	public void registerUserProcessor(UserProcessor<?> userProcessor) {
//		UserProcessor<?> prevUserProcessor = userProcessors.putIfAbsent(userProcessor.interest(), userProcessor);
//		if (prevUserProcessor != null) {
//			throw new RuntimeException("repeat register user processor for " + userProcessor.interest());
//		}
//	}
//
//	public UserProcessor<?> userProcessor(String contentType) {
//		return userProcessors.get(contentType);
//	}
//
//	private void exceptionForMessage(MessageHandlerContext messageHandlerContext, RemotingMessage remotingMessage, Throwable t) {
//		RpcMessageType rpcMessageType = remotingMessage.messageType();
//		String errorMsg = String.format("handle %s message fail, id: %s", rpcMessageType, remotingMessage.id());
//		log.error(errorMsg);
//		if (request == rpcMessageType || heartbeatRequest == rpcMessageType) {
//			RemotingResponseMessage responseMessage = messageHandlerContext.getMessageFactory()
//				.createExceptionResponse(remotingMessage.id(), t, errorMsg);
//			RemotingResponses.sendResponse(messageHandlerContext, responseMessage);
//		}
//	}



}
