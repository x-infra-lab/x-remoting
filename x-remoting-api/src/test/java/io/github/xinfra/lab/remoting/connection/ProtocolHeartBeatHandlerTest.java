package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.netty.handler.timeout.IdleStateEvent.ALL_IDLE_STATE_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ProtocolHeartBeatHandlerTest {

	TestProtocol testProtocol = new TestProtocol();

	@Test
	public void testHeartBeat() throws InterruptedException {
		Heartbeater spyHeartbeater = mock(Heartbeater.class);
		ProtocolHeartBeatHandler protocolHeartBeatHandler = new ProtocolHeartBeatHandler(spyHeartbeater);
		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast(new IdleStateHandler(5, 5, 5, TimeUnit.MILLISECONDS));
		channel.pipeline().addLast(protocolHeartBeatHandler);
		new Connection(testProtocol, channel);

		// simulate IdleStateHandler#fireUserEventTriggered
		channel.pipeline().firstContext().fireUserEventTriggered(ALL_IDLE_STATE_EVENT);

		verify(spyHeartbeater, atLeastOnce()).triggerHeartBeat(any());
	}

}
