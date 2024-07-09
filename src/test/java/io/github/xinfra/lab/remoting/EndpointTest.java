package io.github.xinfra.lab.remoting;

import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;

import static io.github.xinfra.lab.remoting.protocol.RpcProtocol.RPC;


public class EndpointTest {

    @Test
    public void testNewEndpointNormally() {
        String host = "localhost";
        int port = 1234;
        Endpoint endpoint = new Endpoint(RPC, host, port);
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(RPC, endpoint.getProtocolType());
        Assert.assertEquals(host, endpoint.getIp());
        Assert.assertEquals(port, endpoint.getPort());

        Assert.assertNotNull(new Endpoint(RPC, host, 0xFFFF));
        Assert.assertNotNull(new Endpoint(RPC, host, 0));
        Assert.assertNotNull(new Endpoint(RPC, host, RandomUtils.nextInt(0, 0xFFFF)));
    }

    @Test
    public void testNewEndpointError() {
        Assert.assertThrows(NullPointerException.class, () -> {
            new Endpoint(null, "", -1);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(RPC, "", -1);
        });
        Assert.assertThrows(NullPointerException.class, () -> {
            new Endpoint(RPC, null, -1);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(RPC, " ", -1);
        });


        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(RPC, "localhost", -1);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(RPC, "localhost", 0xFFFF + 1);
        });
    }
}
