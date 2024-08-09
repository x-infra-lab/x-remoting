package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionFactory;
import io.github.xinfra.lab.remoting.connection.DefaultConnectionFactory;
import io.github.xinfra.lab.remoting.connection.ServerConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BaseRemotingServerTest {

	TestProtocol testProtocol;

	@BeforeEach
	public void beforeEach() {
		testProtocol = new TestProtocol();
	}

	private Connection getConnection(BaseRemotingServer server) throws RemotingException {
		SocketAddress serverAddress = server.localAddress;

		List<Supplier<ChannelHandler>> channelHandlerSuppliers = new ArrayList<>();
		channelHandlerSuppliers.add(() -> new HttpClientCodec()); // anyone channel
																	// handler is ok
		ConnectionFactory connectionFactory = new DefaultConnectionFactory(testProtocol, channelHandlerSuppliers);
		Connection connection = connectionFactory.create(serverAddress);
		return connection;
	}

	@Test
	public void testBaseRemotingServer1() throws RemotingException, InterruptedException, TimeoutException {
		RemotingServerConfig config = new RemotingServerConfig();
		config.setPort(findAvailableTcpPort());
		config.setManageConnection(false);

		BaseRemotingServer server = new BaseRemotingServer(config) {
			@Override
			public Protocol protocol() {
				return testProtocol;
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
			}
			catch (Throwable e) {
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
			public Protocol protocol() {
				return testProtocol;
			}
		};

		server.startup();

		ServerConnectionManager connectionManager = server.connectionManager;
		Assertions.assertNotNull(connectionManager);

		Connection clientConnection = getConnection(server);
		Assertions.assertNotNull(clientConnection);

		SocketAddress clientAddress = clientConnection.getChannel().localAddress();

		Wait.untilIsTrue(() -> {
			return connectionManager.get(clientAddress) != null;
		}, 30, 100);

		Connection serverConnection = connectionManager.get(clientAddress);
		Assertions.assertNotNull(serverConnection);

		server.shutdown();
	}

	@Test
	public void testRegisterUserProcessor() throws RemotingException, InterruptedException, TimeoutException {
		testProtocol = spy(testProtocol);
		MessageHandler messageHandler = mock(MessageHandler.class);
		doReturn(messageHandler).when(testProtocol).messageHandler();

		RemotingServerConfig config = new RemotingServerConfig();
		config.setPort(findAvailableTcpPort());
		config.setManageConnection(true);

		BaseRemotingServer server = new BaseRemotingServer(config) {
			@Override
			public Protocol protocol() {
				return testProtocol;
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

		verify(messageHandler, times(1)).registerUserProcessor(eq(userProcessor1));
	}

}
