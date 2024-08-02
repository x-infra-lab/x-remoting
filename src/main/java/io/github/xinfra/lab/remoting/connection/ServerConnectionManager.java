package io.github.xinfra.lab.remoting.connection;


import java.net.SocketAddress;
import java.util.concurrent.Future;

public class ServerConnectionManager extends AbstractConnectionManager {

    @Override
    public Connection getOrCreateIfAbsent(SocketAddress socketAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reconnect(SocketAddress socketAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableReconnect(SocketAddress socketAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableReconnect(SocketAddress socketAddress) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Void> asyncReconnect(SocketAddress socketAddress) {
        throw new UnsupportedOperationException();
    }

}
