package io.github.xinfra.lab.remoting.rpc.protocol;

import io.github.xinfra.lab.remoting.rpc.codec.RemotingMessageCodec;
import io.github.xinfra.lab.remoting.rpc.message.RemotingMessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.RemotingRequestMessageHandler;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageDispatcher;

public class RemotingProtocol implements RpcProtocol {

	private final RemotingMessageCodec remotingMessageCodec;

	private final RpcMessageDispatcher rpcMessageDispatcher;

	private final RemotingMessageFactory remotingMessageFactory;

	public RemotingProtocol(RemotingRequestMessageHandler requestMessageHandler) {
		this.remotingMessageCodec = new RemotingMessageCodec();
		this.rpcMessageDispatcher = new RpcMessageDispatcher(requestMessageHandler);
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
	public RpcMessageDispatcher getMessageHandler() {
		return this.rpcMessageDispatcher;
	}

	@Override
	public RemotingMessageFactory getMessageFactory() {
		return this.remotingMessageFactory;
	}

}
