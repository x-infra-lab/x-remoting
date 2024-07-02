package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.processor.UserProcessor;

import java.util.concurrent.ConcurrentHashMap;


public class ClientConnectionManager extends AbstractConnectionManager {

    public ClientConnectionManager(ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        this.connectionFactory = new DefaultConnectionFactory(this, userProcessors);
    }

    public ClientConnectionManager(ConcurrentHashMap<String, UserProcessor<?>> userProcessors,
                                   ConnectionConfig connectionConfig) {
        this.connectionFactory = new DefaultConnectionFactory(this, userProcessors, connectionConfig);
    }

    public ClientConnectionManager(ConnectionManagerConfig config,
                                   ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        super(config);
        this.connectionFactory = new DefaultConnectionFactory(this, userProcessors);
    }

    public ClientConnectionManager(ConnectionManagerConfig config,
                                   ConcurrentHashMap<String, UserProcessor<?>> userProcessors,
                                   ConnectionConfig connectionConfig) {
        super(config);
        this.connectionFactory = new DefaultConnectionFactory(this, userProcessors, connectionConfig);
    }


}
