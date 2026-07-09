package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.client.InFlightRequests;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
		Assertions.assertEquals(connection.getHeartbeatState().getFailCount(), 0);
	}

	@Test
	public void testConnectionWithInvokeFuture() {
		InFlightRequests inFlight = connection.getInFlightRequests();

		final int requestId1 = IDGenerator.nextRequestId();
		Assertions.assertNull(inFlight.remove(requestId1));

		RequestMessage requestMessage1 = mock(RequestMessage.class);
		doReturn(requestId1).when(requestMessage1).getId();

		inFlight.add(new InvokeFuture<>(requestMessage1));
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			inFlight.add(new InvokeFuture<>(requestMessage1));
		});

		final int requestId2 = IDGenerator.nextRequestId();

		RequestMessage requestMessage2 = mock(RequestMessage.class);
		doReturn(requestId2).when(requestMessage2).getId();

		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestMessage2);
		inFlight.add(invokeFuture);

		Assertions.assertEquals(invokeFuture, inFlight.remove(requestId2));
		Assertions.assertNull(inFlight.remove(requestId2));
		Assertions.assertNull(inFlight.remove(requestId2));
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
	public void testOnCloseConnection() {
		MessageFactory messageFactory = mock(MessageFactory.class);
		ResponseMessage connectionClosedMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), any(), eq(ResponseStatus.ConnectionClosed)))
			.thenReturn(connectionClosedMessage);
		testProtocol.setMessageFactory(messageFactory);

		ExecutorService executor = Executors.newCachedThreadPool();
		connection = spy(connection);
		when(connection.getExecutor()).thenReturn(executor);

		InFlightRequests inFlight = connection.getInFlightRequests();

		int times = 10;
		List<InvokeFuture<?>> invokeFutures = new ArrayList<>();
		for (int i = 0; i < times; i++) {
			Integer requestId = IDGenerator.nextRequestId();
			RequestMessage requestMessage = mock(RequestMessage.class);
			doReturn(requestId).when(requestMessage).getId();
			InvokeFuture<ResponseMessage> invokeFuture = new InvokeFuture<>(requestMessage);
			invokeFutures.add(invokeFuture);
			inFlight.add(invokeFuture);
		}
		Assertions.assertEquals(invokeFutures.size(), times);
		Assertions.assertEquals(inFlight.size(), times);

		inFlight.cancelAll(connection.getProtocol(), connection.getExecutor());
		Assertions.assertEquals(0, inFlight.size());
		for (InvokeFuture<?> invokeFuture : invokeFutures) {
			Assertions.assertTrue(invokeFuture.isDone());
		}

		executor.shutdownNow();
	}

}
