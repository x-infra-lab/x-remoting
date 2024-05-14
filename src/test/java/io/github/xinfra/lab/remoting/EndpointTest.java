package io.github.xinfra.lab.remoting;

import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;


public class EndpointTest {

    @Test
    public void testNewEndpoint_Normally() {
        String host = "localhost";
        int port = 1234;
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, host, port);
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(ProtocolType.RPC, endpoint.getProtocolType());
        Assert.assertEquals(host, endpoint.getIp());
        Assert.assertEquals(port, endpoint.getPort());

        Assert.assertNotNull(new Endpoint(ProtocolType.RPC, host, 0xFFFF));
        Assert.assertNotNull(new Endpoint(ProtocolType.RPC, host, 0));
        Assert.assertNotNull(new Endpoint(ProtocolType.RPC, host, RandomUtils.nextInt(0, 0xFFFF)));
    }

    @Test
    public void testNewEndpoint_Error() {
        Assert.assertThrows(NullPointerException.class, () -> {
            new Endpoint(null, "", -1);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(ProtocolType.RPC, "", -1);
        });
        Assert.assertThrows(NullPointerException.class, () -> {
            new Endpoint(ProtocolType.RPC, null, -1);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(ProtocolType.RPC, " ", -1);
        });


        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(ProtocolType.RPC, "localhost", -1);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(ProtocolType.RPC, "localhost", 0xFFFF + 1);
        });
    }
}
