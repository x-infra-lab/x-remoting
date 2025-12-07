package io.github.xinfra.lab.remoting.impl;

import io.github.xinfra.lab.remoting.impl.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.impl.codec.RemotingMessageCodec;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageFactory;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageHandler;

public class RemotingProtocol implements Protocol {

	private final RemotingMessageCodec remotingMessageCodec;

	private final RemotingMessageHandler remotingMessageHandler;

	private final RemotingMessageFactory remotingMessageFactory;

	public RemotingProtocol(RequestHandlerRegistry requestHandlerRegistry) {
		this.remotingMessageCodec = new RemotingMessageCodec();
		this.remotingMessageHandler = new RemotingMessageHandler(requestHandlerRegistry);
		this.remotingMessageFactory = new RemotingMessageFactory();
	}

	@Override
	public RemotingProtocolId getProtocolId() {
		return RemotingProtocolId.INSTANCE;
	}

	@Override
	public RemotingMessageCodec getMessageCodec() {
		return remotingMessageCodec;
	}

	@Override
	public RemotingMessageHandler getMessageHandler() {
		return this.remotingMessageHandler;
	}

	@Override
	public RemotingMessageFactory getMessageFactory() {
		return this.remotingMessageFactory;
	}

}
