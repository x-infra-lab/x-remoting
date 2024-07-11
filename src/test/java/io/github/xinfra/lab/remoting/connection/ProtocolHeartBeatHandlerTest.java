package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.message.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static io.netty.handler.timeout.IdleStateEvent.ALL_IDLE_STATE_EVENT;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ProtocolHeartBeatHandlerTest {

    @Test
    public void testHeartBeat() throws InterruptedException {
        ProtocolType testProtocol = new ProtocolType("testHeartBeat".getBytes());
        ProtocolManager.registerProtocolIfAbsent(testProtocol, new TestProtocol());

        ProtocolHeartBeatHandler protocolHeartBeatHandler = new ProtocolHeartBeatHandler();

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(Connection.PROTOCOL).set(testProtocol);
        channel.pipeline().addLast(new IdleStateHandler(5, 5, 5, TimeUnit.MILLISECONDS));
        channel.pipeline().addLast(protocolHeartBeatHandler);

        HeartbeatTrigger heartbeatTrigger = new HeartbeatTrigger() {
            @Override
            public void triggerHeartBeat(ChannelHandlerContext ctx) {
            }
        };

        HeartbeatTrigger spyHeartbeatTrigger = spy(heartbeatTrigger);
        Protocol protocol = ProtocolManager.getProtocol(testProtocol);
        ((TestProtocol) protocol).setTestHeartbeatTrigger(spyHeartbeatTrigger);

        // simulate IdleStateHandler#fireUserEventTriggered
        channel.pipeline().firstContext().fireUserEventTriggered(ALL_IDLE_STATE_EVENT);

        verify(spyHeartbeatTrigger, atLeastOnce()).triggerHeartBeat(any());
    }
}
