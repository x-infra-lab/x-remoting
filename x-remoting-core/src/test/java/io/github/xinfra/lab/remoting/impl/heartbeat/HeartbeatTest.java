package io.github.xinfra.lab.remoting.impl.heartbeat;

import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.DefaultHeartbeater;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.client.RemotingClient;
import io.github.xinfra.lab.remoting.impl.handler.EchoRequestHandler;
import io.github.xinfra.lab.remoting.impl.handler.ExceptionRequestHandler;
import io.github.xinfra.lab.remoting.impl.server.RemotingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static io.github.xinfra.lab.remoting.impl.handler.RequestApis.echoApi;
import static io.github.xinfra.lab.remoting.impl.handler.RequestApis.exceptionApi;

public class HeartbeatTest {

    private static RemotingServer remotingServer;

    private static RemotingClient remotingClient;

    private static CallOptions callOptions = new CallOptions();

    @BeforeAll
    public static void beforeAll() {
        remotingServer = new RemotingServer();
        remotingServer.startup();

        remotingClient = new RemotingClient();
        remotingClient.startup();
    }

    @AfterAll
    public static void afterAll() {
        remotingServer.shutdown();
        remotingClient.shutdown();
    }

    @Test
    public void testHeartbeat() throws RemotingException, InterruptedException {
        Connection connection = remotingClient.getConnectionManager().connect(remotingServer.localAddress());
        DefaultHeartbeater heartbeatRequest = new DefaultHeartbeater();
        heartbeatRequest.triggerHeartBeat(connection);
        TimeUnit.SECONDS.sleep(1000000);
    }
}
