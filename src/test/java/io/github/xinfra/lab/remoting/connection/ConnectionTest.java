package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.RpcProtocol;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static io.github.xinfra.lab.remoting.connection.Connection.HEARTBEAT_FAIL_COUNT;
import static io.github.xinfra.lab.remoting.connection.Connection.PROTOCOL;
import static org.mockito.Mockito.mock;

public class ConnectionTest {
    private Connection connection;

    @Before
    public void before() {
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, "localhost", 0);
        Channel channel = new EmbeddedChannel();
        connection = new Connection(endpoint, channel);
    }

    @Test
    public void testNewInstance() {
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, "localhost", 0);
        Channel channel = new EmbeddedChannel();
        Connection connection = new Connection(endpoint, channel);

        Assert.assertNotNull(connection);
        Assert.assertEquals(connection.getChannel(), channel);
        Assert.assertEquals(connection.getEndpoint(), endpoint);
        Assert.assertEquals(connection.remoteAddress(), channel.remoteAddress());
        Assert.assertEquals(connection.getChannel().attr(PROTOCOL).get(), endpoint.getProtocolType());
        Assert.assertEquals(connection.getChannel().attr(CONNECTION).get(), connection);
        Assert.assertEquals((long) connection.getChannel().attr(HEARTBEAT_FAIL_COUNT).get(), 0L);
    }

    @Test
    public void testConnectionWithInvokeFuture() {

        final int requestId1 = IDGenerator.nextRequestId();
        Assert.assertNull(connection.removeInvokeFuture(requestId1));

        connection.addInvokeFuture(new InvokeFuture(requestId1, connection));
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            connection.addInvokeFuture(new InvokeFuture(requestId1, connection));
        });


        final int requestId2 = IDGenerator.nextRequestId();
        InvokeFuture invokeFuture = new InvokeFuture(requestId2, connection);
        connection.addInvokeFuture(invokeFuture);

        Assert.assertEquals(invokeFuture, connection.removeInvokeFuture(requestId2));
        Assert.assertNull(connection.removeInvokeFuture(requestId2));
        Assert.assertNull(connection.removeInvokeFuture(requestId2));
    }

    @Test
    public void testCloseConnection() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        connection.close().addListener(future -> {
            Assert.assertTrue(future.isSuccess());
            countDownLatch.countDown();
        });

        countDownLatch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue(countDownLatch.getCount() < 1);
        Assert.assertFalse(connection.getChannel().isActive());
    }

    @Test
    public void testOnCloseConnection() {
        int times = 10;
        List<Integer> requestIds = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            Integer requestId = IDGenerator.nextRequestId();
            requestIds.add(requestId);
            connection.addInvokeFuture(new InvokeFuture(requestId, connection));
        }

        ProtocolManager.registerProtocolIfAbsent(ProtocolType.RPC, new RpcProtocol());
        connection.onClose();

        Assert.assertEquals(requestIds.size(), times);
        for (Integer requestId : requestIds) {
            Assert.assertNull(connection.removeInvokeFuture(requestId));
        }
    }
}
