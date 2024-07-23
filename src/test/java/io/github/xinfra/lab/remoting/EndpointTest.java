package io.github.xinfra.lab.remoting;

import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class EndpointTest {

    private ProtocolType test = new ProtocolType("EndpointTest", "EndpointTest".getBytes());

    @Test
    public void testNewEndpointNormally() {
        String host = "localhost";
        int port = 1234;
        Endpoint endpoint = new Endpoint(test, host, port);
        Assertions.assertNotNull(endpoint);
        Assertions.assertEquals(test, endpoint.getProtocolType());
        Assertions.assertEquals(host, endpoint.getIp());
        Assertions.assertEquals(port, endpoint.getPort());

        Assertions.assertNotNull(new Endpoint(test, host, 0xFFFF));
        Assertions.assertNotNull(new Endpoint(test, host, 0));
        Assertions.assertNotNull(new Endpoint(test, host, RandomUtils.nextInt(0, 0xFFFF)));
    }

    @Test
    public void testNewEndpointError() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new Endpoint(null, "", -1);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(test, "", -1);
        });
        Assertions.assertThrows(NullPointerException.class, () -> {
            new Endpoint(test, null, -1);
        });
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(test, " ", -1);
        });


        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(test, "localhost", -1);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new Endpoint(test, "localhost", 0xFFFF + 1);
        });
    }
}
