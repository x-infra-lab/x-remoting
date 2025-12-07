package io.github.xinfra.lab.remoting.quickstart;

import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.client.RemotingClient;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class QuickStartClient {

    public static void main(String[] args) throws RemotingException, InterruptedException, UnknownHostException {
        RemotingClient client = new RemotingClient();
        client.startup();

        SocketAddress address = new InetSocketAddress("127.0.0.1", 8989);
        System.out.println("call: " + address);
        String msg = client.blockingCall(
                RequestApi.of("echo"),
                new EchoRequest("hello"),
                address,
                new CallOptions()
        );

        System.out.println(msg);
        client.shutdown();
    }
}
