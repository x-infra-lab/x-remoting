package io.github.xinfra.lab.remoting.impl.server;

import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.client.RemotingCall;
import io.github.xinfra.lab.remoting.impl.client.RpcInvokeCallBack;
import io.github.xinfra.lab.remoting.impl.client.RpcInvokeFuture;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandler;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.server.AbstractServer;
import lombok.Getter;

import java.net.SocketAddress;

public class RemotingServer extends AbstractServer {

    @Getter
    private RemotingProtocol protocol;

    private RemotingCall rpcServerRemoting;

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
        rpcServerRemoting = new RemotingCall(connectionManager);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    public <R> R syncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
            throws InterruptedException, RemotingException {
        ensureStarted();

        return rpcServerRemoting.syncCall(requestApi, request, socketAddress, callOptions);
    }

    public <R> RpcInvokeFuture<R> asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
            throws RemotingException {
        ensureStarted();

        return rpcServerRemoting.asyncCall(requestApi, request, socketAddress, callOptions);
    }

    public <R> void asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions,
                              RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
        ensureStarted();

        rpcServerRemoting.asyncCall(requestApi, request, socketAddress, callOptions, rpcInvokeCallBack);
    }

    public void oneway(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions) throws RemotingException {
        ensureStarted();

        rpcServerRemoting.oneway(requestApi, request, socketAddress, callOptions);
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    public <T, R> void registerRequestHandler(RequestApi requestApi, RequestHandler<T, R> userProcessor) {
        requestHandlerRegistry.register(requestApi, userProcessor);
    }

}
