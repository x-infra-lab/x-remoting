package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.rpc.handler.RequestHandlerRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemotingMessageHandler extends AbstractMessageHandler {

	public RemotingMessageHandler(RequestHandlerRegistry requestHandlerRegistry) {
		super();
		registerMessageTypeHandler(new RemotingRequestMessageTypeHandler(requestHandlerRegistry));
	}

}
