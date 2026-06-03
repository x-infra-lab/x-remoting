# Getting Started

x-remoting ships an opinionated RPC implementation (`io.github.xinfra.lab.remoting.impl.*`)
on top of its connection / protocol framework. This page walks through using that
implementation; if you want to build your own protocol on top of the framework, the
implementation itself is the reference.

## Setup

x-remoting is published to Maven Central. Add the dependency:

```xml
<dependency>
    <groupId>io.github.x-infra-lab</groupId>
    <artifactId>x-remoting</artifactId>
    <version>${version}</version>
</dependency>
```

[![Maven Central](https://img.shields.io/maven-central/v/io.github.x-infra-lab/x-remoting)](https://central.sonatype.com/artifact/io.github.x-infra-lab/x-remoting/)

Runs on JDK 8+.

## Quick start

A complete request/response example lives at
[`examples/src/main/java/.../quickstart`](../examples/src/main/java/io/github/xinfra/lab/remoting/quickstart).
Below is the same example, expanded.

### 1. Define a request payload

Request and response objects are serialized over the wire (default: Hessian) and must
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

### 2. Start a server and register a handler

Handlers are keyed by `RequestApi`. The path string is used by the client to address
the handler:

```java
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.server.RemotingServer;
import io.github.xinfra.lab.remoting.impl.server.RemotingServerConfig;

RemotingServerConfig serverConfig = new RemotingServerConfig();
serverConfig.setPort(8989);

RemotingServer server = new RemotingServer(serverConfig);
server.registerRequestHandler(
        RequestApi.of("echo"),
        (EchoRequest request) -> "echo: " + request.getMessage());
server.startup();

System.out.println("server started: " + server.getLocalAddress());
```

`RequestHandler<T, R>` is a `@FunctionalInterface`-style interface; the lambda above
implements `R handle(T request)`. For non-blocking work the default
`asyncHandle(request, observer)` method can be overridden instead.

### 3. Connect a client

```java
import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.impl.client.RemotingClient;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

RemotingClient client = new RemotingClient();
client.startup();

SocketAddress address = new InetSocketAddress("127.0.0.1", 8989);
RequestApi echoApi = RequestApi.of("echo");
```

## Call patterns

`RemotingClient` exposes four call shapes. All four take the same first four arguments
(`api`, `request`, `address`, `CallOptions`); they differ only in how the response is
delivered.

### Blocking

```java
String result = client.blockingCall(
        echoApi,
        new EchoRequest("hello"),
        address,
        new CallOptions());

System.out.println(result);   // → echo: hello
```

### Future

```java
import io.github.xinfra.lab.remoting.impl.client.RemotingFuture;
import java.util.concurrent.TimeUnit;

RemotingFuture<String> future = client.futureCall(
        echoApi, new EchoRequest("hello"), address, new CallOptions());

String result = future.get(3, TimeUnit.SECONDS);
```

### Async / callback

```java
import io.github.xinfra.lab.remoting.impl.client.RemotingCallBack;

client.asyncCall(
        echoApi,
        new EchoRequest("hello"),
        address,
        new CallOptions(),
        new RemotingCallBack<String>() {
            @Override public void onResponse(String response) {
                System.out.println(response);
            }
            @Override public void onException(Throwable t) {
                t.printStackTrace();
            }
        });
```

### Oneway (fire-and-forget)

```java
client.oneway(echoApi, new EchoRequest("hello"), address, new CallOptions());
```

No response is delivered and no `InvokeFuture` is tracked.

## `CallOptions`

| Field                | Default                  | Notes                                     |
|----------------------|--------------------------|-------------------------------------------|
| `timeoutMills`       | `3000`                   | Per-call timeout in milliseconds.         |
| `serializationType`  | `SerializationType.Hession` | Per-call serializer.                   |
| `headers`            | empty `DefaultMessageHeaders` | Application-defined message headers. |

## Server-to-client calls

`RemotingServer` exposes the **same** `blockingCall` / `futureCall` / `asyncCall` /
`oneway` API. Once a client has connected, the server can route a request back to that
client's address — handy for push notifications and reverse RPC:

```java
SocketAddress clientAddress = /* obtained from a prior request's Connection */;
server.oneway(RequestApi.of("notify"), new Notification("..."), clientAddress, new CallOptions());
```

For this to work the server's `RemotingServerConfig.manageConnection` must be `true`
so the server keeps accepted connections in its `ServerConnectionManager`.

## Configuration

The `RemotingClient` no-arg constructor uses sensible defaults. To tune behaviour,
pass a `RemotingClientConfig`:

All `*Config` classes are immutable and built via fluent builders. Construction validates
the inputs (positive timeouts, non-null backoff policy, etc.) and fails fast:

```java
import io.github.xinfra.lab.remoting.connection.*;
import io.github.xinfra.lab.remoting.impl.client.RemotingClientConfig;

ConnectionFactoryConfig factoryConfig = ConnectionFactoryConfig.builder()
        .connectTimeout(2000)
        .build();

ConnectionManagerConfig managerConfig = ConnectionManagerConfig.builder()
        .connectionNumPerEndpoint(3)
        .build();

ReconnectConfig reconnectConfig = ReconnectConfig.builder()
        .maxAttempts(20)
        .build();

RemotingClientConfig clientConfig = new RemotingClientConfig();
clientConfig.setConnectionFactoryConfig(factoryConfig);
clientConfig.setConnectionManagerConfig(managerConfig);
clientConfig.setReconnectConfig(reconnectConfig);

RemotingClient client = new RemotingClient(clientConfig);
client.startup();
```

See the related docs for what each knob controls:

- [ConnectionManager](./connection-manager.md) — pool, lifecycle, events, heartbeats
- [Reconnect](./reconnect.md) — backoff policy, retry caps, listeners

## Shutdown

Always shut down in reverse order of construction:

```java
client.shutdown();   // disconnects all endpoints, stops reconnector, closes Netty groups
server.shutdown();   // closes server channel, drains accepted connections
```

Both `shutdown()` calls are one-shot — invoking them twice throws
`IllegalStateException`.
