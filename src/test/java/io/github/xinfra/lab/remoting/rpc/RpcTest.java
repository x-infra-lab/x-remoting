package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;
import static io.github.xinfra.lab.remoting.protocol.ProtocolType.RPC;

public class RpcTest {

    private RpcServer rpcServer;

    private RpcClient rpcClient;

    @Before
    public void beforeClass() {
        rpcServer = new RpcServer(findAvailableTcpPort());
        rpcServer.startup();
        rpcServer.registerUserProcessor(new SimpleUserProcessor());


        rpcClient = new RpcClient();
        rpcClient.startup();
    }


    @Test
    public void testBasicCall1() {
        InetSocketAddress remoteAddress = rpcServer.localAddress();

        try {
            String result = rpcClient.syncCall(new SimpleRequest("test"),
                    new Endpoint(RPC, remoteAddress.getHostName(), remoteAddress.getPort()),
                    3000);
            Assert.assertEquals("echo:test", result);
        } catch (RemotingException e) {
            Assert.fail(e.getMessage());
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }


}
