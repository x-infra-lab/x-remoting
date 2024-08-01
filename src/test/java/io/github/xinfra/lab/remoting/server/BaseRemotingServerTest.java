package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.SocketAddress;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionFactory;
import io.github.xinfra.lab.remoting.connection.DefaultConnectionFactory;
import io.github.xinfra.lab.remoting.connection.ServerConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
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
        SocketAddress socketAddress = new SocketAddress(test, serverAddress.getHostName(), serverAddress.getPort());
        Connection connection = connectionFactory.create(socketAddress);
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
        Wait.untilIsTrue(() -> {
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

        ServerConnectionManager connectionManager = server.connectionManager;
        Assertions.assertNotNull(connectionManager);

        Connection clientConnection = getConnection(server);
        Assertions.assertNotNull(clientConnection);

        InetSocketAddress clientAddress = ((InetSocketAddress) clientConnection.getChannel().localAddress());

        SocketAddress socketAddress = new SocketAddress(test, clientAddress.getHostName(), clientAddress.getPort());
        Wait.untilIsTrue(() -> {
            return connectionManager.get(socketAddress) != null;
        }, 30, 100);

        Connection serverConnection = connectionManager.get(socketAddress);
        Assertions.assertNotNull(serverConnection);

        server.shutdown();
    }

    @Test
    public void testRegisterUserProcessor() throws RemotingException, InterruptedException, TimeoutException {
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

        UserProcessor<String> userProcessor1 = new UserProcessor<String>() {
            @Override
            public String interest() {
                return String.class.getName();
            }

            @Override
            public Object handRequest(String request) {
                // do nothing
                return null;
            }
        };

        server.registerUserProcessor(userProcessor1);

        UserProcessor<String> userProcessor2 = new UserProcessor<String>() {
            @Override
            public String interest() {
                return String.class.getName();
            }

            @Override
            public Object handRequest(String request) {
                // do nothing
                return null;
            }
        };

        Assertions.assertThrows(RuntimeException.class, () -> {
            server.registerUserProcessor(userProcessor2);
        });

        server.shutdown();
    }
}
