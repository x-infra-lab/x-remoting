package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.util.concurrent.Future;

public class ServerConnectionManager extends AbstractConnectionManager {

    @Override
    public Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Void> reconnect(Endpoint endpoint) throws RemotingException {
        throw new UnsupportedOperationException();
    }
}
