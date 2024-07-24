package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.client.RpcClient;
import io.github.xinfra.lab.remoting.rpc.server.RpcServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;
import static io.github.xinfra.lab.remoting.rpc.RpcProtocol.RPC;

public class RpcTest {

    private static RpcServer rpcServer;

    private static RpcClient rpcClient;

    @BeforeAll
    public static void beforeClass() {
        rpcServer = new RpcServer(findAvailableTcpPort());
        rpcServer.startup();
        rpcServer.registerUserProcessor(new SimpleUserProcessor());


        rpcClient = new RpcClient();
        rpcClient.startup();
    }

    @AfterAll
    public static void afterClass(){
        rpcServer.shutdown();
        rpcClient.shutdown();
    }


    @Test
    public void testBasicCall1() {
        InetSocketAddress remoteAddress = rpcServer.localAddress();

        try {
            String result = rpcClient.syncCall(new SimpleRequest("test"),
                    new Endpoint(RPC, remoteAddress.getHostName(), remoteAddress.getPort()),
                    1000);
            Assertions.assertEquals("echo:test", result);

            result = rpcClient.syncCall(new SimpleRequest("test"),
                    new Endpoint(RPC, remoteAddress.getHostName(), remoteAddress.getPort()),
                    1000);
            Assertions.assertEquals("echo:test", result);

            result = rpcClient.syncCall(new SimpleRequest("test"),
                    new Endpoint(RPC, remoteAddress.getHostName(), remoteAddress.getPort()),
                    1000);
            Assertions.assertEquals("echo:test", result);

        } catch (RemotingException e) {
            Assertions.fail(e.getMessage());
        } catch (InterruptedException e) {
            Assertions.fail(e.getMessage());
        }

    }


}
