package io.github.xinfra.lab.remoting.impl.server;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.client.RemotingCall;
import io.github.xinfra.lab.remoting.impl.processor.UserProcessor;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.client.RpcInvokeCallBack;
import io.github.xinfra.lab.remoting.impl.client.RpcInvokeFuture;
import io.github.xinfra.lab.remoting.server.AbstractServer;
import lombok.Getter;

import java.net.SocketAddress;

public class RemotingServer extends AbstractServer {

	@Getter
	private RemotingProtocol protocol;

	private RemotingCall rpcServerRemoting;

	public RemotingServer() {
		super(new RemotingServerConfig());
	}

	public RemotingServer(RemotingServerConfig config) {
		super(config);
	}

	@Override
	public void startup() {
		super.startup();
		protocol = new RemotingProtocol();
		rpcServerRemoting = new RemotingCall(connectionManager);
	}

	@Override
	public void shutdown() {
		super.shutdown();
	}

	public <R> R syncCall(Object request, SocketAddress socketAddress, int timeoutMills)
			throws InterruptedException, RemotingException {
		ensureStarted();

		return rpcServerRemoting.syncCall(request, socketAddress, timeoutMills);
	}

	public <R> RpcInvokeFuture<R> asyncCall(Object request, SocketAddress socketAddress, int timeoutMills)
			throws RemotingException {
		ensureStarted();

		return rpcServerRemoting.asyncCall(request, socketAddress, timeoutMills);
	}

	public <R> void asyncCall(Object request, SocketAddress socketAddress, int timeoutMills,
			RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
		ensureStarted();

		rpcServerRemoting.asyncCall(request, socketAddress, timeoutMills, rpcInvokeCallBack);
	}

	public void oneway(Object request, SocketAddress socketAddress) throws RemotingException {
		ensureStarted();

		rpcServerRemoting.oneway(request, socketAddress);
	}

	@Override
	public Protocol protocol() {
		return protocol;
	}

	public void registerUserProcessor(UserProcessor<?> userProcessor) {
		protocol().messageHandler().registerUserProcessor(userProcessor);
	}

}
