# RPC Usage

> [📖 Index](README.md) · Previous: [← Architecture](architecture.md) · Next: [Configuration Reference →](configuration-reference.md) · [🇨🇳 中文](rpc-usage.zh-CN.md)

The `core` module ships a complete RPC built on top of the framework. This page
covers the entry-point APIs you'll touch day-to-day.

## RemotingClient

```java
RemotingClient client = new RemotingClient();         // sensible defaults
// or:
RemotingClient client = new RemotingClient(clientConfig);

client.startup();
// ... calls ...
client.shutdown();
```

The no-arg constructor uses `ConnectionFactoryConfig.defaults()`,
`ConnectionManagerConfig.defaults()`, and `ReconnectConfig.defaults()`. See
[Configuration Reference](configuration-reference.md) for what you can change.

### Four call patterns

Each call takes the same prefix: `(RequestApi, request, InetSocketAddress, CallOptions)`.
What differs is how the response gets delivered.

#### `blockingCall` — wait inline

```java
String result = client.blockingCall(
        RequestApi.of("echo"),
        new EchoRequest("hi"),
        address,
        CallOptions.defaults());
```

The thread blocks until the response arrives or the per-call timeout fires
(`CallOptions.timeoutMills`, default 3000 ms). Throws `RemotingException` on
transport / serialization errors and `ResponseStatusRuntimeException` for an
error response from the server.

#### `futureCall` — get a `RemotingFuture<R>`

```java
RemotingFuture<String> future = client.futureCall(
        RequestApi.of("echo"),
        new EchoRequest("hi"),
        address,
        CallOptions.defaults());

String result = future.get(3, TimeUnit.SECONDS);   // or future.get()
boolean done = future.isDone();
```

#### `asyncCall` — register a callback

```java
client.asyncCall(
        RequestApi.of("echo"),
        new EchoRequest("hi"),
        address,
        CallOptions.defaults(),
        new RemotingCallBack<String>() {
            @Override public void onResponse(String response) { /* ... */ }
            @Override public void onException(Throwable t)   { /* ... */ }
        });
```

The callback runs on the connection's executor
(`RemotingClient-Client-Default-Executor-*`). Don't block this thread.

#### `oneway` — fire and forget

```java
client.oneway(RequestApi.of("notify"), new Notify("..."), address, CallOptions.defaults());
```

No response is delivered and no `InvokeFuture` is tracked. Best for events where
the caller doesn't need to know whether the peer received them.

## RemotingServer

```java
RemotingServerConfig serverConfig = new RemotingServerConfig();
serverConfig.setPort(8989);

RemotingServer server = new RemotingServer(serverConfig);

server.registerRequestHandler(
        RequestApi.of("echo"),
        (EchoRequest req) -> "echo: " + req.getMessage());

server.startup();
```

### Async handler

`RequestHandler<T, R>` has a default `asyncHandle(request, ResponseObserver<R>)` that
defers completion. Override it for non-blocking work:

```java
server.registerRequestHandler(RequestApi.of("slow"), new RequestHandler<SlowReq, String>() {
    @Override public String handle(SlowReq req) { throw new UnsupportedOperationException(); }

    @Override public void asyncHandle(SlowReq req, ResponseObserver<String> obs) {
        someService.fetchAsync(req).whenComplete((res, err) -> {
            if (err != null) obs.onError(err);
            else             obs.complete(res);
        });
    }

    @Override public Executor getExecutor() { return myAppExecutor; }
});
```

When the handler returns a custom `Executor` from `getExecutor()`, the framework
dispatches the handler invocation onto that executor instead of the server's default.

### Server-to-client calls

`RemotingServer` exposes the **same** `blockingCall` / `futureCall` / `asyncCall` /
`oneway` methods. Once a client has connected, the server can route a request back
to that client's local address:

```java
RemotingServerConfig serverConfig = new RemotingServerConfig();
serverConfig.setPort(8989);
serverConfig.setManageConnection(true);          // ← required for reverse calls
RemotingServer server = new RemotingServer(serverConfig);

// later, given an InetSocketAddress reachable on the client...
server.oneway(RequestApi.of("notify"), new Notify("hello"), clientAddress, callOptions);
```

`manageConnection=true` makes the server retain accepted connections in its
`ServerConnectionManager`. Without it, the server only routes inbound requests and
cannot initiate calls.

The client side must register handlers for the routes the server may push to:

```java
client.registerRequestHandler(RequestApi.of("notify"), (Notify n) -> { /* ... */ });
```

## CallOptions

```java
CallOptions opts = CallOptions.builder()
        .timeoutMills(5000)
        .serializationType(SerializationType.Hession)
        .build();

// or use all defaults:
CallOptions opts = CallOptions.defaults();
```

- `timeoutMills` — per-call timeout. The framework schedules a timeout in the
  `HashedWheelTimer` and completes the `InvokeFuture` with a `Timeout` status if it
  fires before the response.
- `serializationType` — currently only `Hession` is shipped; the SPI is enum-based,
  open to extension.
- `headers` — arbitrary string headers attached to the request message; the server
  handler can read them off the request body.

## Connection lifecycle (RPC view)

```
client.startup()
   └── ClientConnectionManager.startup()
          ├── ConnectionEventProcessor.startup()
          └── Reconnector.startup()        // timer + worker pool

(first call to addr A)
   └── ConnectionManager.get(A)
          ├── new Connection (Netty bootstrap.connect)
          ├── add to connectionsMap[A]
          └── fire ConnectionEvent.CONNECT  ──▶ listeners

(channel drops)
   └── Netty channelInactive ─▶ ConnectionManager.close(conn)
          ├── invalidate conn from connectionsMap[A]
          ├── fire ConnectionEvent.CLOSE     ──▶ listeners
          └── reconnector.onUnhealthy(A)
                 └── schedule attempt with backoff …

client.shutdown()
   └── ClientConnectionManager.shutdown()
          ├── disconnect all addresses
          ├── connectionFactory.close()   // shuts down Netty groups
          └── reconnector.shutdown()      // cancels timers, drains workers
```

For the gory details, see [Connection Manager](connection-manager.md) and
[Reconnect](reconnect.md).

## Common mistakes

- **Forgetting `manageConnection=true` for reverse calls.** Without it, the server
  has no connection to route to and you'll get a `RemotingException`.
- **Blocking inside `RemotingCallBack`.** It runs on the client's I/O executor pool.
  Trampoline to your own pool for any non-trivial work.
- **Reusing `EchoRequest`-style POJOs across versions without compatible Hessian
  serialization.** Hessian uses field names; add fields cautiously.
- **Calling `shutdown()` twice.** `AbstractLifeCycle` is CAS-guarded and throws
  `IllegalStateException` on the second call.

---

> [📖 Index](README.md) · Previous: [← Architecture](architecture.md) · Next: [Configuration Reference →](configuration-reference.md)
