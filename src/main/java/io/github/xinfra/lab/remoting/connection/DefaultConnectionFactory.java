package io.github.xinfra.lab.remoting.connection;

public class DefaultConnectionFactory extends AbstractConnectionFactory {
    private ConnectionManager connectionManager;

    public DefaultConnectionFactory(ConnectionManager connectionManager) {
        super(new ConnectionEventHandler(connectionManager),
                new ProtocolEncoder(),
                new ProtocolDecoder(),
                new ProtocolHeartBeatHandler(),
                new ProtocolHandler()
        );
        this.connectionManager = connectionManager;
    }
}
