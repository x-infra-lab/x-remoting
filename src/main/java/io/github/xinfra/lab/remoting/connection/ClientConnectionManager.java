package io.github.xinfra.lab.remoting.connection;


import io.github.xinfra.lab.remoting.processor.UserProcessor;
import java.util.concurrent.ConcurrentHashMap;


public class ClientConnectionManager extends AbstractConnectionManager {

    public ClientConnectionManager(ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        this.connectionFactory = new DefaultConnectionFactory(this, userProcessors);
    }

}
