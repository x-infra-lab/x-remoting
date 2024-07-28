package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.Until;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionFactory;
import io.github.xinfra.lab.remoting.connection.DefaultConnectionFactory;
import io.github.xinfra.lab.remoting.connection.ServerConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BaseRemotingServerTest {

    private static ProtocolType test = new ProtocolType("BaseRemotingServerTest",
            new byte[]{0xb});

    private Connection getConnection(BaseRemotingServer server) throws RemotingException {
        InetSocketAddress serverAddress = server.localAddress;

        List<Supplier<ChannelHandler>> channelHandlerSuppliers = new ArrayList<>();
        channelHandlerSuppliers.add(() -> new HttpClientCodec()); // anyone channel handler is ok
        ConnectionFactory connectionFactory = new DefaultConnectionFactory(channelHandlerSuppliers);
        Endpoint endpoint = new Endpoint(test, serverAddress.getHostName(), serverAddress.getPort());
        Connection connection = connectionFactory.create(endpoint);
        return connection;
    }

    @Test
    public void testBaseRemotingServer1() throws RemotingException, InterruptedException, TimeoutException {
        RemotingServerConfig config = new RemotingServerConfig();
        config.setPort(findAvailableTcpPort());
        config.setManageConnection(false);

        BaseRemotingServer server = new BaseRemotingServer(config) {
            @Override
            public ProtocolType protocolType() {
                return test;
            }
        };
        server = spy(server);

        server.startup();

        Assertions.assertNull(server.connectionManager);
        Connection connection = getConnection(server);
        Assertions.assertNotNull(connection);

        BaseRemotingServer finalServer = server;
        Until.untilIsTrue(() -> {
            try {
                verify(finalServer, atLeastOnce()).createConnection(any());
                return true;
            } catch (Throwable e) {
                return false;
            }
        }, 30, 100);

        verify(finalServer, times(1)).createConnection(any());

        server.shutdown();
    }


    @Test
    public void testBaseRemotingServer2() throws RemotingException, InterruptedException, TimeoutException {
        RemotingServerConfig config = new RemotingServerConfig();
        config.setPort(findAvailableTcpPort());
        config.setManageConnection(true);

        BaseRemotingServer server = new BaseRemotingServer(config) {
            @Override
            public ProtocolType protocolType() {
                return test;
            }
        };

        server.startup();
        InetSocketAddress serverAddress = server.localAddress;

        ServerConnectionManager connectionManager = server.connectionManager;
        Assertions.assertNotNull(connectionManager);

        Connection clientConnection = getConnection(server);
        Assertions.assertNotNull(clientConnection);

        InetSocketAddress clientAddress = ((InetSocketAddress) clientConnection.getChannel().localAddress());

        Endpoint endpoint = new Endpoint(test, clientAddress.getHostName(), clientAddress.getPort());
        Until.untilIsTrue(() -> {
            return connectionManager.get(endpoint) != null;
        }, 30, 100);

        Connection serverConnection = connectionManager.get(endpoint);
        Assertions.assertNotNull(serverConnection);

        server.shutdown();
    }
}
