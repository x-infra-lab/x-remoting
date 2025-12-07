package io.github.xinfra.lab.remoting.quickstart;

import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.server.RemotingServer;
import io.github.xinfra.lab.remoting.impl.server.RemotingServerConfig;

public class QuickStartServer {



    public static void main(String[] args) throws InterruptedException {
        RemotingServerConfig serverConfig = new RemotingServerConfig();
        serverConfig.setPort(8989);
//        serverConfig.setHostName("127.0.0.1");
        RemotingServer server = new RemotingServer(serverConfig);

        server.registerRequestHandler(RequestApi.of("echo"),
                    (EchoRequest request) -> {
                        return new String("echo: " + request.message);
                    }
                );
        server.startup();

        System.out.println("server started:" + server.getLocalAddress());

        Thread.currentThread().join();
    }
}
