package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;


public interface ConnectionManager {

    Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException;

    Connection get(Endpoint endpoint);

    void check(Connection connection) throws RemotingException;

    void remove(Connection connection);

    void add(Connection connection);
}
