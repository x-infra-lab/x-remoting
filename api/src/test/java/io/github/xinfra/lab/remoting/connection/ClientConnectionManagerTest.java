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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
	public void testGetFail() {
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

		connectionManager.reconnector().disable(address);
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

		connectionManager.reconnector().disable(address);
		connectionManager.close(connection);
		Assertions.assertNull(((ClientConnectionManager) connectionManager).connectionsMap.get(address));
		// close again
		connectionManager.close(connection);

	}

	@Test
	public void testReconnect1() throws RemotingException, InterruptedException, TimeoutException {
		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Connection connection = connectionManager.get(address);
		Assertions.assertNotNull(connection);

		Map<InetSocketAddress, Connections> connections = ((ClientConnectionManager) connectionManager).connectionsMap;
		Assertions.assertTrue(connections.containsKey(address));
		connectionManager.close(connection);

		Assertions.assertTrue(!connections.containsKey(address));

		connectionManager.reconnector().onUnhealthy(address);
		Wait.untilIsTrue(() -> {
			if (connections.containsKey(address)) {
				return true;
			}
			return false;
		}, 100, 50);

		Assertions.assertTrue(connections.containsKey(address));
	}

	@Test
	public void testReconnect2() throws RemotingException, InterruptedException, TimeoutException {
		int numPerEndpoint = 3;
		ConnectionManagerConfig connectionManagerConfig = ConnectionManagerConfig.builder()
			.connectionNumPerEndpoint(numPerEndpoint)
			.build();
		ConnectionManager connectionManager = new ClientConnectionManager(testProtocol, connectionManagerConfig);
		connectionManager.startup();

		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Connection connection = connectionManager.get(address);

		Map<InetSocketAddress, Connections> connectionsMap = ((ClientConnectionManager) connectionManager).connectionsMap;
		Connections connections = connectionsMap.get(address);
		Assertions.assertEquals(connections.size(), numPerEndpoint);

		connectionManager.close(connection);
		Assertions.assertEquals(connections.size(), numPerEndpoint - 1);

		connectionManager.reconnector().onUnhealthy(address);
		Wait.untilIsTrue(() -> {
			if (Objects.equals(connections.size(), numPerEndpoint)) {
				return true;
			}
			return false;
		}, 100, 50);

		Assertions.assertEquals(connections.size(), numPerEndpoint);
	}

	@Test
	public void testReconnect3() throws InterruptedException, RemotingException, TimeoutException {
		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Map<InetSocketAddress, Connections> connections = ((ClientConnectionManager) connectionManager).connectionsMap;

		Reconnector reconnector = connectionManager.reconnector();

		ConnectionManager spyConnectionManager = spy(connectionManager);
		((DefaultReconnector) reconnector).connectionManager = spyConnectionManager;

		Assertions.assertTrue(!connections.containsKey(address));

		reconnector.onUnhealthy(address);

		Wait.untilIsTrue(() -> {
			if (connections.containsKey(address)) {
				return true;
			}
			return false;
		}, 100, 50);

		verify(spyConnectionManager, times(1)).connect(eq(address));
	}

	@Test
	void testDisableReconnect() throws InterruptedException, TimeoutException {
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);
		Reconnector reconnector = connectionManager.reconnector();
		Map<InetSocketAddress, Connections> connections = ((ClientConnectionManager) connectionManager).connectionsMap;

		reconnector.disable(address);
		reconnector.onUnhealthy(address);

		// DISABLED ignores onUnhealthy; state stays DISABLED and no connection is made.
		Wait.untilIsTrue(() -> {
			return reconnector.stateOf(address) == ReconnectState.DISABLED && !connections.containsKey(address);
		}, 100, 30);

		reconnector.enable(address);
		reconnector.onUnhealthy(address);

		// After enable + onUnhealthy, the task schedules a reconnect; once it succeeds
		// the state returns to IDLE and the address appears in the map.
		Wait.untilIsTrue(() -> {
			return reconnector.stateOf(address) == ReconnectState.IDLE && connections.containsKey(address);
		}, 100, 50);
	}

	@Test
	void testConnectionEventListener() throws RemotingException, InterruptedException, TimeoutException {

		connectionManager.connectionEventProcessor().addConnectionEventListener(new ConnectionEventListener() {
			@Override
			public void onEvent(ConnectionEvent connectionEvent, Connection connection) {
				// threw exception will not affect others listener
				throw new RuntimeException("test throw exception");
			}
		});

		AtomicBoolean connectFlag = new AtomicBoolean(false);
		AtomicReference<Connection> connectionRef1 = new AtomicReference<>();
		connectionManager.connectionEventProcessor().addConnectionEventListener(new ConnectionEventListener() {
			@Override
			public void onEvent(ConnectionEvent connectionEvent, Connection connection) {
				if (ConnectionEvent.CONNECT == connectionEvent) {
					connectionRef1.set(connection);
					connectFlag.set(true);
				}
			}
		});

		AtomicBoolean closeFlag = new AtomicBoolean(false);
		AtomicReference<Connection> connectionRef2 = new AtomicReference<>();
		connectionManager.connectionEventProcessor().addConnectionEventListener(new ConnectionEventListener() {
			@Override
			public void onEvent(ConnectionEvent connectionEvent, Connection connection) {
				if (ConnectionEvent.CLOSE == connectionEvent) {
					connectionRef2.set(connection);
					closeFlag.set(true);
				}
			}
		});

		// valid socketAddress
		InetSocketAddress address = new InetSocketAddress(remoteAddress, serverPort);

		// create connection
		Connection connection = connectionManager.get(address);
		Assertions.assertNotNull(connection);

		Wait.untilIsTrue(() -> {
			return connectFlag.get();
		}, 30, 100);

		Assertions.assertSame(connection, connectionRef1.get());

		connectionManager.reconnector().disable(address);
		connectionManager.close(connection);

		Wait.untilIsTrue(() -> {
			return closeFlag.get();
		}, 30, 100);

		Assertions.assertSame(connection, connectionRef2.get());
	}

}
