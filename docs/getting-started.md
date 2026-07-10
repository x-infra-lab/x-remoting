# Getting Started

> [📖 Index](README.md) · Next: [Architecture →](architecture.md) · [🇨🇳 中文](getting-started.zh-CN.md)

This page takes you from an empty `pom.xml` to a working server/client roundtrip in
under five minutes.

## Prerequisites

- JDK 8 or newer (the CI matrix covers 8, 11, 17, 21)
- Maven 3.6+
- Network access to Maven Central

## 1. Add the dependency

```xml
<dependency>
    <groupId>io.github.x-infra-lab</groupId>
    <artifactId>x-remoting</artifactId>
    <version><!-- latest from Maven Central --></version>
</dependency>
```

[![Maven Central](https://img.shields.io/maven-central/v/io.github.x-infra-lab/x-remoting)](https://central.sonatype.com/artifact/io.github.x-infra-lab/x-remoting/)

x-remoting depends on `netty-handler` and `netty-transport-classes-epoll`. The native
Epoll binary is **not** pulled in by default — to use it on Linux for the best
throughput, add:

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-transport-native-epoll</artifactId>
    <classifier>linux-x86_64</classifier>
    <!-- or linux-aarch64 -->
</dependency>
```

If the native lib is absent, x-remoting falls back to NIO transparently.

## 2. Define a request payload

Request and response objects travel over the wire (Hessian by default), so they must
be `Serializable`:

```java
import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class EchoRequest implements Serializable {
    private String message;
}
```

## 3. Start a server

Handlers are registered by path. The path string is the route the client uses
to address the handler.

```java
import io.github.xinfra.lab.remoting.rpc.server.RemotingServer;
import io.github.xinfra.lab.remoting.rpc.server.RemotingServerConfig;

RemotingServerConfig serverConfig = new RemotingServerConfig();
serverConfig.setPort(8989);

RemotingServer server = new RemotingServer(serverConfig);
server.registerRequestHandler("echo",
        BlockingRequestHandler.of((EchoRequest req) -> "echo: " + req.getMessage()));
server.startup();

System.out.println("server listening on " + server.getLocalAddress());
```

`RequestHandler<T, R>` has a single method `void handle(T, ResponseObserver<R>)`.
For synchronous handlers, use `BlockingRequestHandler.of(fn)` or extend
`BlockingRequestHandler<T, R>`. For non-blocking work, implement `RequestHandler`
directly and call `observer.complete(result)` when ready.

## 4. Send a request

```java
import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.rpc.client.RemotingClient;

import java.net.InetSocketAddress;

RemotingClient client = new RemotingClient();
client.startup();

InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8989);

String result = client.blockingCall(
        "echo",
        new EchoRequest("hello"),
        address,
        CallOptions.defaults());

System.out.println(result);   // → echo: hello
```

Shut down in reverse order when you're done:

```java
client.shutdown();
server.shutdown();
```

## What just happened

```
┌─────────────────┐                                      ┌─────────────────┐
│  RemotingClient │   ┌──── TCP (Netty) ─────────────┐   │  RemotingServer │
│                 │ ──▶ EchoRequest (Hessian-encoded) ──▶│                 │
│  blockingCall() │                                       │  echo handler   │
│                 │ ◀── RemotingResponseMessage ─────── ◀ │                 │
└─────────────────┘                                      └─────────────────┘
```

x-remoting did several things on your behalf:

1. Opened a pooled TCP connection to `127.0.0.1:8989` (kept warm for next call)
2. Installed an `IdleStateHandler` and started heartbeating every 15 seconds
3. Encoded the request via the `RemotingProtocol` (length-prefixed binary frame +
   Hessian payload), assigned a request id, and tracked the `InvokeFuture`
4. Routed the response back to your `blockingCall` thread
5. Set up a [Reconnect](reconnect.md) state machine so that a transient TCP drop is
   handled automatically next time you call

## Next steps

- Understand the moving parts in **[Architecture →](architecture.md)**
- See more call patterns (future, async, oneway) in [RPC Usage](rpc-usage.md)
- Tune the connection pool, heartbeat and reconnect behaviour in
  [Configuration Reference](configuration-reference.md)

---

> [📖 Index](README.md) · Next: [Architecture →](architecture.md)
