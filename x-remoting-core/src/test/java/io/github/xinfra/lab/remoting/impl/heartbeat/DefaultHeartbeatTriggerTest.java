package io.github.xinfra.lab.remoting.impl.heartbeat;

import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.DefaultHeartbeater;
import io.github.xinfra.lab.remoting.impl.message.RpcMessageType;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DefaultHeartbeatTriggerTest {

	private RemotingProtocol protocol;

	@BeforeEach
	public void beforeEach() {
		protocol = new RemotingProtocol(handlerRegistry);
	}

	@AfterEach
	public void afterEach() throws IOException {
		protocol.close();
	}

	@Test
	public void testHeartbeat() throws InterruptedException, TimeoutException {
		DefaultHeartbeater trigger = protocol.heartbeatTrigger();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		channel = spy(channel);
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());
		new Connection(protocol, channel);

		trigger.triggerHeartBeat(context);

		EmbeddedChannel finalChannel = channel;
		Wait.untilIsTrue(() -> {
			try {
				verify(finalChannel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		// verify request
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingRequestMessage>() {
			@Override
			public boolean matches(RemotingRequestMessage argument) {

				if (!Objects.equals(argument.messageType(), RpcMessageType.heartbeatRequest)) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testHeartbeatFailed() throws InterruptedException, TimeoutException {
		DefaultHeartbeater trigger = protocol.heartbeatTrigger();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		channel = spy(channel);
		doReturn(channel).when(context).channel();
		doReturn(channel.newFailedFuture(new RuntimeException("testHeartbeatFailed"))).when(channel)
			.writeAndFlush(any());
		Connection connection = new Connection(protocol, channel);
		connection = spy(connection);
		channel.attr(CONNECTION).set(connection);

		trigger.triggerHeartBeat(context);
		Connection finalConnection = connection;
		Wait.untilIsTrue(() -> {
			try {
				return finalConnection.getHeartbeatFailCnt() > 0;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		Assertions.assertEquals(connection.getHeartbeatFailCnt(), 1);

		// again
		trigger.triggerHeartBeat(context);
		Wait.untilIsTrue(() -> {
			try {
				return finalConnection.getHeartbeatFailCnt() > 1;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		Assertions.assertEquals(connection.getHeartbeatFailCnt(), 2);

		// again
		trigger.triggerHeartBeat(context);
		Wait.untilIsTrue(() -> {
			try {
				return finalConnection.getHeartbeatFailCnt() > 2;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		Assertions.assertEquals(connection.getHeartbeatFailCnt(), 3);

		// again
		trigger.triggerHeartBeat(context);
		Wait.untilIsTrue(() -> {
			try {
				return finalConnection.getHeartbeatFailCnt() > 3;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		Assertions.assertEquals(connection.getHeartbeatFailCnt(), 4);

		// again
		trigger.triggerHeartBeat(context);
		verify(connection, times(1)).close();
	}

	@Test
	public void testHeartbeatOverThreshold() throws InterruptedException, TimeoutException {
		DefaultHeartbeater trigger = protocol.heartbeatTrigger();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		channel = spy(channel);
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());
		Connection connection = new Connection(protocol, channel);
		connection = spy(connection);
		channel.attr(CONNECTION).set(connection);

		connection.setHeartbeatFailCnt(4);

		trigger.triggerHeartBeat(context);

		verify(connection, times(1)).close();
	}

}
