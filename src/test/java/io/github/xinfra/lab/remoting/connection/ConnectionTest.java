package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class ConnectionTest {

	private Connection connection;

	private TestProtocol testProtocol;

	private Channel channel;

	@BeforeEach
	public void before() {
		testProtocol = new TestProtocol();
		channel = new EmbeddedChannel();
		connection = new Connection(testProtocol, channel, mock(Executor.class), mock(Timer.class));
	}

	@Test
	public void testNewInstance() {
		Assertions.assertNotNull(connection);
		Assertions.assertEquals(connection.getChannel(), channel);
		Assertions.assertEquals(connection.remoteAddress(), channel.remoteAddress());
		Assertions.assertEquals(connection.getProtocol(), testProtocol);
		Assertions.assertEquals(channel.attr(CONNECTION).get(), connection);
	}

	@Test
	public void testCloseConnection() throws InterruptedException {
		connection = spy(connection);

		connection.close().sync();
		Assertions.assertFalse(connection.getChannel().isActive());

		connection.close().sync();
		Assertions.assertFalse(connection.getChannel().isActive());
	}

	@Test
	public void testCloseHook() throws InterruptedException {
		AtomicBoolean hookCalled = new AtomicBoolean(false);
		connection.addCloseHook(() -> hookCalled.set(true));

		connection.close().sync();
		Assertions.assertTrue(hookCalled.get());
	}

}
