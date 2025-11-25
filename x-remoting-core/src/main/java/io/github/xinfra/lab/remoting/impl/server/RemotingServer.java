package io.github.xinfra.lab.remoting.impl.server;

import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.client.RemotingCall;
import io.github.xinfra.lab.remoting.impl.client.RemotingCallBack;
import io.github.xinfra.lab.remoting.impl.client.RemotingFuture;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandler;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.server.AbstractServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

@Slf4j
public class RemotingServer extends AbstractServer {

	@Getter
	private RemotingProtocol protocol;

	private RemotingCall serverRemotingCall;

	private RequestHandlerRegistry requestHandlerRegistry = new RequestHandlerRegistry();

	public RemotingServer() {
		super(new RemotingServerConfig());
	}

	public RemotingServer(RemotingServerConfig config) {
		super(config);
	}

	@Override
	public void startup() {
		super.startup();
		protocol = new RemotingProtocol(requestHandlerRegistry);
		serverRemotingCall = new RemotingCall(connectionManager);
	}

	@Override
	public void shutdown() {
		super.shutdown();
	}

	public <R> R syncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws InterruptedException, RemotingException {
		ensureStarted();

		return serverRemotingCall.syncCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> RemotingFuture<R> asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException {
		ensureStarted();

		return serverRemotingCall.asyncCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress,
			CallOptions callOptions, RemotingCallBack<R> remotingCallBack) throws RemotingException {
		ensureStarted();

		serverRemotingCall.asyncCall(requestApi, request, socketAddress, callOptions, remotingCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {
		ensureStarted();

		serverRemotingCall.oneway(requestApi, request, socketAddress, callOptions);
	}

	public <T, R> void registerRequestHandler(RequestApi requestApi, RequestHandler<T, R> userProcessor) {
		requestHandlerRegistry.register(requestApi, userProcessor);
	}

}
