package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.RpcProtocol;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class InvokeFutureTest {
    private InvokeFuture invokeFuture;

    @BeforeClass
    public static void beforeClass() {
        ProtocolManager.registerProtocolIfAbsent(ProtocolType.RPC, new RpcProtocol());
    }

    @Before
    public void before() {
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, "localhost", 0);
        Channel channel = new EmbeddedChannel();
        Connection connection = new Connection(endpoint, channel);

        final int requestId1 = IDGenerator.nextRequestId();
        invokeFuture = new InvokeFuture(requestId1, connection);
    }

    @Test
    public void testTimeout() {
        Assert.assertNull(invokeFuture.getTimeout());
        Assert.assertFalse(invokeFuture.cancelTimeout());

        HashedWheelTimer timer = new HashedWheelTimer();

        Timeout timeout = timer.newTimeout(t -> {
        }, 3, TimeUnit.SECONDS);
        invokeFuture.addTimeout(timeout);
        Assert.assertEquals(invokeFuture.getTimeout(), timeout);

        Assert.assertTrue(invokeFuture.cancelTimeout());
        Assert.assertFalse(invokeFuture.cancelTimeout());
        Assert.assertTrue(invokeFuture.getTimeout().isCancelled());

        Assert.assertThrows(IllegalArgumentException.class, ()->{
            invokeFuture.addTimeout(timeout);
        });
    }
}
