package io.github.xinfra.lab.remoting.rpc.server;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.rpc.client.IDGenerator;
import io.github.xinfra.lab.remoting.rpc.client.RemotingCall;
import io.github.xinfra.lab.remoting.rpc.client.RemotingCallBack;
import io.github.xinfra.lab.remoting.rpc.client.RemotingFuture;
import io.github.xinfra.lab.remoting.rpc.handler.RequestHandler;
import io.github.xinfra.lab.remoting.rpc.message.RemotingRequestMessageHandler;
import io.github.xinfra.lab.remoting.rpc.message.ResponseMessage;
import io.github.xinfra.lab.remoting.rpc.protocol.RemotingProtocol;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.github.xinfra.lab.remoting.server.AbstractServer;
import io.github.xinfra.lab.remoting.server.ServerConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RemotingServer extends AbstractServer {

	@Getter
	private final RemotingProtocol protocol;

	private final RemotingCall serverRemotingCall;

	private final RemotingRequestMessageHandler requestMessageHandler = new RemotingRequestMessageHandler();

	public RemotingServer() {
		this(ServerConfig.defaults());
	}

	public RemotingServer(ServerConfig config) {
		super(config);
		this.protocol = new RemotingProtocol(requestMessageHandler);
		this.serverRemotingCall = new RemotingCall(connectionManager, new IDGenerator());
	}

	public <R> R blockingCall(String path, Object request, InetSocketAddress socketAddress, CallOptions callOptions)
			throws InterruptedException, RemotingException {
		ensureStarted();

		return serverRemotingCall.blockingCall(path, request, socketAddress, callOptions);
	}

	public <R> RemotingFuture<R> futureCall(String path, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException {
		ensureStarted();

		return serverRemotingCall.futureCall(path, request, socketAddress, callOptions);
	}

	public <R> void asyncCall(String path, Object request, InetSocketAddress socketAddress, CallOptions callOptions,
			RemotingCallBack<R> remotingCallBack) throws RemotingException {
		ensureStarted();

		serverRemotingCall.asyncCall(path, request, socketAddress, callOptions, remotingCallBack);
	}

	public void oneway(String path, Object request, InetSocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {
		ensureStarted();

		serverRemotingCall.oneway(path, request, socketAddress, callOptions);
	}

	@Override
	public void shutdown() {
		if (connectionManager != null) {
			sendGoaway();
		}
		super.shutdown();
	}

	private void sendGoaway() {
		List<ChannelFuture> futures = new ArrayList<>();
		connectionManager.forEachConnection(conn -> {
			ResponseMessage goaway = protocol.getMessageFactory().createGoaway(SerializationType.Hessian);
			try {
				goaway.serialize();
			}
			catch (Exception e) {
				log.warn("failed to serialize goaway message for {}", conn.remoteAddress(), e);
				return;
			}
			futures.add(conn.getChannel().writeAndFlush(goaway));
		});
		for (ChannelFuture f : futures) {
			try {
				f.await(3, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	public <T, R> void registerRequestHandler(String path, RequestHandler<T, R> requestHandler) {
		requestMessageHandler.register(path, requestHandler);
	}

}
