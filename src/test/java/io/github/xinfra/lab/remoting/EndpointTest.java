package io.github.xinfra.lab.remoting;

import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;



public class EndpointTest {

    private ProtocolType test = new ProtocolType("EndpointTest", "EndpointTest".getBytes());

    @Test
    public void testNewEndpointNormally() {
        String host = "localhost";
        int port = 1234;
        Endpoint endpoint = new Endpoint(test, host, port);
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(test, endpoint.getProtocolType());
        Assert.assertEquals(host, endpoint.getIp());
        Assert.assertEquals(port, endpoint.getPort());

        Assert.assertNotNull(new Endpoint(test, host, 0xFFFF));
        Assert.assertNotNull(new Endpoint(test, host, 0));
        Assert.assertNotNull(new Endpoint(test, host, RandomUtils.nextInt(0, 0xFFFF)));
    }

    @Test
    public void testNewEndpointError() {
        Assert.assertThrows(NullPointerException.class, () -> {
            new Endpoint(null, "", -1);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(test, "", -1);
        });
        Assert.assertThrows(NullPointerException.class, () -> {
            new Endpoint(test, null, -1);
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(test, " ", -1);
        });


        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(test, "localhost", -1);
        });

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(test, "localhost", 0xFFFF + 1);
        });
    }
}
