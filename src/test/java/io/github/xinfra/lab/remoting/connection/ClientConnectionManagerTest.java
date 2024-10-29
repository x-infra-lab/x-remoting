package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.TestServerUtils;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientConnectionManagerTest {

	private ConnectionManager connectionManager;

	private static String remoteAddress;

	private static int serverPort;

	private static Protocol testProtocol = new TestProtocol();

	private static NioServerSocketChannel serverSocketChannel;

	@BeforeAll
	public static void beforeAll() throws InterruptedException {
		serverSocketChannel = TestServerUtils.startEmptyServer();
		remoteAddress = serverSocketChannel.localAddress().getHostName();
		serverPort = serverSocketChannel.localAddress().getPort();
	}

	@AfterAll
	public static void afterAll() throws InterruptedException {
		serverSocketChannel.close().sync();
	}

	@BeforeEach
	public void before() {
		connectionManager = new ClientConnectionManager(testProtocol);
		Assertions.assertNotNull(connectionManager);
		connectionManager.startup();
	}

	@AfterEach
	public void after() {
		connectionManager.shutdown();
	}

	@Test
	public void testNewInstance() {

		Assertions.assertThrows(NullPointerException.class, () -> {
			new ClientConnectionManager(null);
		});

	}

	@Test
	public void testGetOrCreateIfAbsent() throws RemotingException {
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Connection connection1 = connectionManager.get(address);
		Assertions.assertNotNull(connection1);

		Connection connection2 = connectionManager.get(address);
		Assertions.assertTrue(connection1 == connection2);
	}

	@Test
	public void testGetOrCreateIfAbsentFail() {
		// invalid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort + 1);
		Assertions.assertThrows(RemotingException.class, () -> {
			connectionManager.get(address);
		});
	}

	@Test
	public void testGet() throws RemotingException {
		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);

		// create connection
		Connection connection1 = connectionManager.get(address);
		Assertions.assertNotNull(connection1);

		Connection connection2 = connectionManager.get(address);
		Assertions.assertNotNull(connection1);

		Assertions.assertTrue(connection1 == connection2);
	}

	@Test
	public void testGetFail() throws RemotingException {
		// invalid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort + 1);

		// fail create connection
		Assertions.assertThrows(RemotingException.class, () -> {
			connectionManager.get(address);
		});

	}

	@Test
	public void testCheck() throws RemotingException {
		Assertions.assertThrows(NullPointerException.class, () -> {
			connectionManager.check(null);
		});

		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Connection connection = connectionManager.get(address);
		connectionManager.check(connection);
	}

	@Test
	public void testCheckWritable() throws RemotingException {
		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Connection connection = connectionManager.get(address);
		connectionManager.check(connection);

		// mock
		Channel channel = mock(Channel.class);
		when(channel.isWritable()).thenReturn(false);
		when(channel.isActive()).thenReturn(true);

		Connection spyConnection = spy(connection);
		doReturn(channel).when(spyConnection).getChannel();

		Assertions.assertThrows(RemotingException.class, () -> {
			connectionManager.check(spyConnection);
		});

		Connection connection1 = connectionManager.get(address);
		Assertions.assertNotNull(connection1);
		Assertions.assertTrue(connection1 == connection);

	}

	@Test
	public void testCheckActive() throws RemotingException {
		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Connection connection = connectionManager.get(address);
		connectionManager.check(connection);

		// mock
		Channel channel = mock(Channel.class);
		when(channel.isActive()).thenReturn(false);

		Connection spyConnection = spy(connection);
		doReturn(channel).when(spyConnection).getChannel();

		ConnectionManager spyConnectionManager = spy(connectionManager);

		connectionManager.reconnector().disableReconnect(address);
		Assertions.assertThrows(RemotingException.class, () -> {
			spyConnectionManager.check(spyConnection);
		});

		verify(spyConnectionManager, times(1)).close(eq(spyConnection));
	}

	@Test
	public void testCloseConnection() throws RemotingException {
		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Connection connection = connectionManager.get(address);

		connectionManager.reconnector().disableReconnect(address);
		connectionManager.close(connection);
		Assertions.assertNull(((ClientConnectionManager) connectionManager).connections.get(address));
		// close again
		connectionManager.close(connection);

	}

	@Test
	public void testReconnect1() throws RemotingException, InterruptedException, TimeoutException {
		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Connection connection = connectionManager.get(address);
		Assertions.assertNotNull(connection);

		Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;
		Assertions.assertTrue(connections.containsKey(address));
		connections.remove(address);

		connectionManager.reconnector().reconnect(address);
		Wait.untilIsTrue(() -> {
			if (connections.containsKey(address)) {
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(connections.containsKey(address));
	}

	@Test
	public void testReconnect2() throws RemotingException, InterruptedException, TimeoutException {
		int numPreEndpoint = 3;
		ConnectionManagerConfig connectionManagerConfig = new ConnectionManagerConfig();
		connectionManagerConfig.setConnectionNumPreEndpoint(numPreEndpoint);
		ConnectionManager connectionManager = new ClientConnectionManager(testProtocol, connectionManagerConfig);
		connectionManager.startup();

		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Connection connection = connectionManager.get(address);

		Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;
		ConnectionHolder connectionHolder = connections.get(address);
		Assertions.assertEquals(connectionHolder.size(), numPreEndpoint);

		connectionHolder.connections.remove(connection);
		Assertions.assertEquals(connectionHolder.size(), numPreEndpoint - 1);

		connectionManager.reconnector().reconnect(address);
		Wait.untilIsTrue(() -> {
			if (Objects.equals(connectionHolder.size(), numPreEndpoint)) {
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertEquals(connectionHolder.size(), numPreEndpoint);
	}

	@Test
	public void testReconnect3() throws InterruptedException, RemotingException, TimeoutException {
		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;

		Reconnector reconnector = connectionManager.reconnector();

		connectionManager = spy(connectionManager);
		((DefaultReconnector) reconnector).connectionManager = connectionManager;

		reconnector.reconnect(address);

		Wait.untilIsTrue(() -> {
			if (connections.containsKey(address)) {
				return true;
			}
			return false;
		}, 30, 100);

		verify(connectionManager, times(1)).connect(eq(address));
	}

	@Test
	public void testReconnect4()
			throws InterruptedException, RemotingException, TimeoutException, UnknownHostException {
		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;

		Connection connection = connectionManager.get(address);
		connectionManager.close(connection);
		Assertions.assertTrue(!connections.containsKey(address));

		Wait.untilIsTrue(() -> {
			ConnectionHolder connectionHolder = connections.get(address);
			if (connectionHolder != null && connectionHolder.get() != null) {
				return true;
			}
			return false;
		}, 100, 30);

		Assertions.assertTrue(connections.containsKey(address));
		connection = connectionManager.get(address);
		Assertions.assertNotNull(connection);
	}

	@Test
	void testDisableReconnect() throws RemotingException, ExecutionException, InterruptedException, TimeoutException {
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Reconnector reconnector = connectionManager.reconnector();
		Map<SocketAddress, ConnectionHolder> connections = ((ClientConnectionManager) connectionManager).connections;

		reconnector.disableReconnect(address);
		reconnector.reconnect(address);

		Wait.untilIsTrue(() -> {
			return ((DefaultReconnector) reconnector).reconnectAddressQueue.isEmpty()
					&& !connections.containsKey(address);
		}, 100, 30);

		reconnector.enableReconnect(address);
		reconnector.reconnect(address);

		Wait.untilIsTrue(() -> {
			return ((DefaultReconnector) reconnector).reconnectAddressQueue.isEmpty()
					&& connections.containsKey(address);
		}, 100, 30);
	}

}
