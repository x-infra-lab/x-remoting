package io.github.xinfra.lab.remoting.rpc.protocol;

import io.github.xinfra.lab.remoting.rpc.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.rpc.codec.RemotingMessageCodec;
import io.github.xinfra.lab.remoting.rpc.message.RemotingMessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.RemotingMessageHandler;

public class RemotingProtocol implements RpcProtocol {

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
