package io.github.xinfra.lab.remoting.impl.processor;

import io.github.xinfra.lab.remoting.impl.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessage;
import io.github.xinfra.lab.remoting.impl.message.RpcResponses;

public class RpcHeartbeatMessageProcessor extends AbstractMessageProcessor<RemotingMessage> {

	@Override
	public void handleMessage(MessageHandlerContext messageHandlerContext, RemotingMessage message) {
		RpcResponses.sendResponse(messageHandlerContext,
				messageHandlerContext.getMessageFactory().createResponse(message.id(), null));
	}

}
