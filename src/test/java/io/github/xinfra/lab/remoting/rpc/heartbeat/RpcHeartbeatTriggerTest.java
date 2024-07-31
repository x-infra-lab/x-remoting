package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.RpcRequestMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static io.github.xinfra.lab.remoting.connection.Connection.HEARTBEAT_FAIL_COUNT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RpcHeartbeatTriggerTest {
    static {
        ProtocolManager.registerProtocolIfAbsent(RpcProtocol.RPC, new RpcProtocol());
    }

    @Test
    public void testHeartbeat() throws InterruptedException, TimeoutException {
        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();
        RpcHeartbeatTrigger trigger = new RpcHeartbeatTrigger(rpcMessageFactory);

        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        EmbeddedChannel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(channel).when(context).channel();

        Connection connection = mock(Connection.class);
        doReturn(channel).when(connection).getChannel();
        channel.attr(CONNECTION).set(connection);
        channel.attr(HEARTBEAT_FAIL_COUNT).set(0);
        doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());

        trigger.triggerHeartBeat(context);

        EmbeddedChannel finalChannel = channel;
        Wait.untilIsTrue(() -> {
            try {
                verify(finalChannel, atLeastOnce()).writeAndFlush(any());
                return true;
            } catch (Throwable t) {
                return false;
            }
        }, 30, 100);

        // verify request
        verify(channel, times(1)).writeAndFlush(argThat(
                new ArgumentMatcher<RpcRequestMessage>() {
                    @Override
                    public boolean matches(RpcRequestMessage argument) {


                        if (!Objects.equals(argument.messageType(), MessageType.heartbeatRequest)) {
                            return false;
                        }
                        return true;
                    }
                }
        ));
    }


    @Test
    public void testHeartbeatFailed() throws InterruptedException, TimeoutException {
        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();
        RpcHeartbeatTrigger trigger = new RpcHeartbeatTrigger(rpcMessageFactory);

        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        EmbeddedChannel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(channel).when(context).channel();

        Endpoint endpoint = new Endpoint(RpcProtocol.RPC, "localhost", 8080);
        Connection connection = new Connection(endpoint, channel);
        connection = spy(connection);
        channel.attr(CONNECTION).set(connection);

        doReturn(channel.newFailedFuture(new RuntimeException("testHeartbeatFailed"))).when(channel).writeAndFlush(any());

        trigger.triggerHeartBeat(context);
        EmbeddedChannel finalChannel = channel;
        Wait.untilIsTrue(() -> {
            try {
                return finalChannel.attr(HEARTBEAT_FAIL_COUNT).get() > 0;
            } catch (Throwable t) {
                return false;
            }
        }, 30, 100);

        Assertions.assertEquals(channel.attr(HEARTBEAT_FAIL_COUNT).get(), 1);

        // again
        trigger.triggerHeartBeat(context);
        Wait.untilIsTrue(() -> {
            try {
                return finalChannel.attr(HEARTBEAT_FAIL_COUNT).get() > 1;
            } catch (Throwable t) {
                return false;
            }
        }, 30, 100);

        Assertions.assertEquals(channel.attr(HEARTBEAT_FAIL_COUNT).get(), 2);

        // again
        trigger.triggerHeartBeat(context);
        Wait.untilIsTrue(() -> {
            try {
                return finalChannel.attr(HEARTBEAT_FAIL_COUNT).get() > 2;
            } catch (Throwable t) {
                return false;
            }
        }, 30, 100);

        Assertions.assertEquals(channel.attr(HEARTBEAT_FAIL_COUNT).get(), 3);

        // again
        trigger.triggerHeartBeat(context);
        Wait.untilIsTrue(() -> {
            try {
                return finalChannel.attr(HEARTBEAT_FAIL_COUNT).get() > 3;
            } catch (Throwable t) {
                return false;
            }
        }, 30, 100);

        Assertions.assertEquals(channel.attr(HEARTBEAT_FAIL_COUNT).get(), 4);

        // again
        trigger.triggerHeartBeat(context);
        verify(connection, times(1)).close();
    }

    @Test
    public void testHeartbeatOverThreshold() throws InterruptedException, TimeoutException {
        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();
        RpcHeartbeatTrigger trigger = new RpcHeartbeatTrigger(rpcMessageFactory);

        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        EmbeddedChannel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(channel).when(context).channel();

        Connection connection = mock(Connection.class);
        doReturn(channel).when(connection).getChannel();
        channel.attr(CONNECTION).set(connection);
        channel.attr(HEARTBEAT_FAIL_COUNT).set(4);
        doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());

        trigger.triggerHeartBeat(context);

        verify(connection, times(1)).close();
    }
}
