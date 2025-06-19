package io.github.xinfra.lab.remoting.rpc.processor;

import io.github.xinfra.lab.remoting.rpc.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponses;

public class RpcHeartbeatMessageProcessor extends AbstractMessageProcessor<RpcMessage> {

	@Override
	public void handleMessage(MessageHandlerContext messageHandlerContext, RpcMessage message) {
		RpcResponses.sendResponse(messageHandlerContext,
				messageHandlerContext.getMessageFactory().createResponse(message.id(), null));
	}

}
