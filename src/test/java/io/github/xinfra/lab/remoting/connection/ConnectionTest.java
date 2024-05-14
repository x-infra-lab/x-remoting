package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static io.github.xinfra.lab.remoting.connection.Connection.HEARTBEAT_FAIL_COUNT;
import static io.github.xinfra.lab.remoting.connection.Connection.PROTOCOL;

public class ConnectionTest {

    @Test
    public void test() {
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, "localhost", 0);
        Channel channel = new EmbeddedChannel();
        Connection connection = new Connection(endpoint, channel);

        Assert.assertNotNull(connection);
        Assert.assertEquals(connection.getChannel(), channel);
        Assert.assertEquals(connection.getEndpoint(), endpoint);
        Assert.assertEquals(connection.getChannel().attr(PROTOCOL).get(), endpoint.getProtocolType());
        Assert.assertEquals(connection.getChannel().attr(CONNECTION).get(), connection);
        Assert.assertEquals((long) connection.getChannel().attr(HEARTBEAT_FAIL_COUNT).get(), 0L);
    }
}
