package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;

import java.util.concurrent.Future;

public class ServerConnectionManager extends AbstractConnectionManager {

    @Override
    public Connection getOrCreateIfAbsent(Endpoint endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reconnect(Endpoint endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Void> asyncReconnect(Endpoint endpoint) {
        throw new UnsupportedOperationException();
    }
}
