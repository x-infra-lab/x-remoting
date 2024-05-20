package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

public class InvokeFutureTest {
    private InvokeFuture invokeFuture;

    @Before
    public void before() {
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, "localhost", 0);
        Channel channel = new EmbeddedChannel();
        Connection connection = new Connection(endpoint, channel);

        final int requestId1 = IDGenerator.nextRequestId();
        invokeFuture = new InvokeFuture(requestId1, connection);
    }

    @Test
    public void testNewInstance() {
//        invokeFuture.
    }
}
