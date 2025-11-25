package io.github.xinfra.lab.remoting.connection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class ConnectionSelectStrategyTest {

	@Test
	public void testRoundRobin() {
		RoundRobinConnectionSelectStrategy connectionSelectStrategy = new RoundRobinConnectionSelectStrategy();

		Assertions.assertNull(connectionSelectStrategy.select(null));
		Assertions.assertNull(connectionSelectStrategy.select(new ArrayList<>()));

		List<Connection> connectionList = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			connectionList.add(mock(Connection.class));
		}

		Assertions.assertTrue(connectionList.contains(connectionSelectStrategy.select(connectionList)));
		Assertions.assertTrue(connectionList.contains(connectionSelectStrategy.select(connectionList)));
		Assertions.assertTrue(connectionList.contains(connectionSelectStrategy.select(connectionList)));
	}

}
