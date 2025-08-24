package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.message.AbstractMessageHandler;
import io.github.xinfra.lab.remoting.message.Message;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class RemotingMessageHandler extends AbstractMessageHandler {


    public RemotingMessageHandler() {
        // request
//		RpcRequestMessageProcessor rpcRequestMessageProcessor = new RpcRequestMessageProcessor();
//		this.registerMessageProcessor(RpcMessageType.request, rpcRequestMessageProcessor);
//		this.registerMessageProcessor(RpcMessageType.onewayRequest, rpcRequestMessageProcessor);
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
