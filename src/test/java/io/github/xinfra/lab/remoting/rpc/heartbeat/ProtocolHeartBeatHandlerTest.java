package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.protocol.TestRpcProtocol;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.Timer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;

import static io.netty.handler.timeout.IdleStateEvent.ALL_IDLE_STATE_EVENT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProtocolHeartBeatHandlerTest {

	@Test
	public void testHeartBeat() {
		Heartbeater spyHeartbeater = mock(Heartbeater.class);
		ProtocolHeartBeatHandler protocolHeartBeatHandler = new ProtocolHeartBeatHandler(spyHeartbeater);
		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast(protocolHeartBeatHandler);
		new Connection(new TestRpcProtocol(), channel, mock(Executor.class), mock(Timer.class));

		channel.pipeline().fireUserEventTriggered(ALL_IDLE_STATE_EVENT);

		verify(spyHeartbeater, atLeastOnce()).triggerHeartBeat(any());
	}

}
