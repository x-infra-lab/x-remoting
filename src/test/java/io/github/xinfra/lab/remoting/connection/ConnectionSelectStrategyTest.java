package io.github.xinfra.lab.remoting.connection;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class ConnectionSelectStrategyTest {

    @Test
    public void testRoundRobin() {
        RoundRobinConnectionSelectStrategy connectionSelectStrategy = new RoundRobinConnectionSelectStrategy();

        Assert.assertNull(connectionSelectStrategy.select(null));
        Assert.assertNull(connectionSelectStrategy.select(new ArrayList<>()));

        List<Connection> connectionList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            connectionList.add(mock(Connection.class));
        }

        Assert.assertTrue(connectionList.contains(connectionSelectStrategy.select(connectionList)));
        Assert.assertTrue(connectionList.contains(connectionSelectStrategy.select(connectionList)));
        Assert.assertTrue(connectionList.contains(connectionSelectStrategy.select(connectionList)));
    }
}
