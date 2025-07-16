package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.rpc.processor.MessageProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.UserProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.RpcHeartbeatMessageProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.RpcRequestMessageProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.RpcResponseMessageProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.github.xinfra.lab.remoting.rpc.message.RpcMessageType.heartbeatRequest;
import static io.github.xinfra.lab.remoting.rpc.message.RpcMessageType.request;

@Slf4j
public class RpcMessageHandler implements MessageHandler {

	private Timer timer;

	private ConcurrentHashMap<RpcMessageType, MessageProcessor<RpcMessage>> messageProcessors = new ConcurrentHashMap<>();

	private ConcurrentHashMap<String, UserProcessor<?>> userProcessors = new ConcurrentHashMap<>();

	// TODO: use config
	private ExecutorService executor = new ThreadPoolExecutor(20, 400, 60, TimeUnit.SECONDS,
			new ArrayBlockingQueue<Runnable>(1024), new NamedThreadFactory("Rpc-Message-Handler"));

	public RpcMessageHandler() {
		this.timer = null;// todo deleted it

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
	public ExecutorService executor(ResponseMessage responseMessage) {
		return executor;
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

	@Override
	public Timer timer() {
		return timer;
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
