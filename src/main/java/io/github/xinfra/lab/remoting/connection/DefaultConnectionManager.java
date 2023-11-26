package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;


public class DefaultConnectionManager implements ConnectionManager {

    private ConnectionFactory connectionFactory;


    public DefaultConnectionManager(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }


    @Override
    public Connection getConnection(Endpoint endpoint) throws RemotingException {
        // TODO
        return connectionFactory.create(endpoint);
    }
}
