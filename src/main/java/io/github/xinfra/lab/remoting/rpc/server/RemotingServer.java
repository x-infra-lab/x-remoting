package io.github.xinfra.lab.remoting.rpc.server;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.rpc.client.RemotingCall;
import io.github.xinfra.lab.remoting.rpc.client.RemotingCallBack;
import io.github.xinfra.lab.remoting.rpc.client.RemotingFuture;
import io.github.xinfra.lab.remoting.rpc.handler.RequestApi;
import io.github.xinfra.lab.remoting.rpc.handler.RequestHandler;
import io.github.xinfra.lab.remoting.rpc.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.rpc.protocol.RemotingProtocol;
import io.github.xinfra.lab.remoting.server.AbstractServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class RemotingServer extends AbstractServer {

	@Getter
	private final RemotingProtocol protocol;

	private final RemotingCall serverRemotingCall;

	private final RequestHandlerRegistry requestHandlerRegistry = new RequestHandlerRegistry();

	public RemotingServer() {
		this(new RemotingServerConfig());
	}

	public RemotingServer(RemotingServerConfig config) {
		super(config);
		this.protocol = new RemotingProtocol(requestHandlerRegistry);
		this.serverRemotingCall = new RemotingCall(connectionManager);
	}

	public <R> R blockingCall(RequestApi requestApi, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions) throws InterruptedException, RemotingException {
		ensureStarted();

		return serverRemotingCall.blockingCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> RemotingFuture<R> futureCall(RequestApi requestApi, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException {
		ensureStarted();

		return serverRemotingCall.futureCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions, RemotingCallBack<R> remotingCallBack) throws RemotingException {
		ensureStarted();

		serverRemotingCall.asyncCall(requestApi, request, socketAddress, callOptions, remotingCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, InetSocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {
		ensureStarted();

		serverRemotingCall.oneway(requestApi, request, socketAddress, callOptions);
	}

	public <T, R> void registerRequestHandler(RequestApi requestApi, RequestHandler<T, R> userProcessor) {
		requestHandlerRegistry.register(requestApi, userProcessor);
	}

}
