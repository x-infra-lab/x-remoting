# FAQ

> [📖 Index](README.md) · Previous: [← Extending](extending.md) · Next: [Design Debt →](design-debt.md) · [🇨🇳 中文](faq.zh-CN.md)

## Why does the public API take `InetSocketAddress` instead of `SocketAddress`?

x-remoting only supports TCP. Using `InetSocketAddress` in the API removes a round of
casts for users and makes the supported transport explicit. The framework's
internal `Connection.remoteAddress()` still returns `SocketAddress` (Netty's
contract) and casts at the boundary in `AbstractConnectionManager`.

## When does the heartbeat actually fire?

It's driven by Netty's `IdleStateHandler`, installed when
`ConnectionFactoryConfig.idleSwitch=true` (default). The handler fires when none of
read / write / both reach the configured threshold (default 15 s). On the event,
`DefaultHeartbeater` sends a heartbeat request and increments a fail counter on
failure; after `heartbeatMaxFailCount` consecutive failures (default 3), the
connection is closed and the reconnect pipeline kicks in.

If you want longer-lived idle periods without disconnects, lower the threshold or
disable the idle check entirely with `idleSwitch=false`.

## What's the difference between `disconnect`, `cancel`, `disable`, and `close`?

| Operation                                  | Effect on pool                | Effect on reconnect           |
|--------------------------------------------|-------------------------------|-------------------------------|
| `ConnectionManager.close(connection)`      | Remove one connection         | Trigger `onUnhealthy(addr)`   |
| `ConnectionManager.disconnect(addr)`       | Remove entire pool for addr   | Trigger `cancel(addr)`        |
| `Reconnector.cancel(addr)`                 | (no effect on pool)           | Reset state to IDLE           |
| `Reconnector.disable(addr)`                | (no effect on pool)           | Pause; `onUnhealthy` ignored  |
| `Reconnector.onUnhealthy(addr)`            | (no effect on pool)           | Schedule reconnect            |

Use `disconnect` when the user says "I'm done with this endpoint". Use `close` for a
single dead connection (the framework does this automatically on channel inactive).
Use `disable` to temporarily pause reconnects (e.g. during planned maintenance) and
`enable` to resume.

## Why does reconnect take 1+ second to fire?

The default `BackoffPolicy` is `ExponentialBackoffWithJitter` starting at 1 s with
±50% jitter (so the first delay is ~500 ms–1.5 s). It's deliberate — instant retry
amplifies failure-mode load on the remote. If you need faster recovery, swap in
`FixedIntervalBackoff` with a short interval or a custom policy. If you need a
**synchronous** reconnect, call `ConnectionManager.get(addr)` directly.

## How do I plug in OpenTelemetry / Micrometer?

For reconnect events: implement `ReconnectListener` and increment your meters in the
callbacks (see [Extending](extending.md)).

For connection-level events: implement `ConnectionEventListener` and register it
with `connectionEventProcessor()`.

There is no first-class metrics integration yet — it's on the roadmap.

## Can I use unix domain sockets / TLS / HTTP transport?

Not directly. The shipped `ConnectionFactory` uses TCP only and the public API takes
`InetSocketAddress`. You can write a custom `ConnectionFactory` (and possibly a
custom `Protocol`) on top of the `api` module to support other transports.

## How do I get a server to call back into a client?

Set `RemotingServerConfig.manageConnection=true`. The server then retains accepted
channels in its `ServerConnectionManager`. Once a client has connected, the server
can call `server.blockingCall(api, request, clientAddress, opts)` and route the
request back over that channel. See [RPC Usage](rpc-usage.md#server-to-client-calls).

## Why does `shutdown()` throw `IllegalStateException` the second time I call it?

`AbstractLifeCycle.shutdown()` is CAS-guarded — it transitions the lifecycle state
exactly once. If you might call `shutdown()` from multiple cleanup paths, guard
with `isStarted()` first:

```java
if (client.isStarted()) {
    client.shutdown();
}
```

## How do I share Netty threads across multiple clients?

Not directly through the RPC API. You can pass a shared `Executor` and `Timer` via
`ConnectionFactoryConfig` — those will at least be shared. For a fully shared
`EventLoopGroup`, drop down to the `api` module and construct a custom
`ConnectionFactory`. First-class support is on the roadmap.

## Does it work on macOS / Windows?

Yes — both for development and production. On Linux, adding the
`netty-transport-native-epoll:linux-x86_64` (or `:linux-aarch64`) dependency
enables Epoll for higher throughput. Without it, x-remoting falls back to NIO.

## How do I add a custom serializer (Kryo, Protobuf, …)?

Currently `SerializationType` is an enum and only `Hession` ships. Swapping
serializers is a roadmap item — the registry needs to become an SPI. Until then,
you can hand-roll a request payload that's already serialized into a `byte[]`
field, and unwrap on the server side.

## My listener throws — does it crash other listeners?

No. The dispatcher wraps every listener invocation in `try { ... } catch
(Throwable t) { log.warn(...); }`. The exception is logged and the next listener
runs. Same for `ReconnectListener`s.

## Does x-remoting create daemon threads?

The Netty `EventLoopGroup`s are daemon threads (Netty default), but the
`RemotingClient-Reconnect-Worker-*` pool, the event dispatcher thread, and the
server's cached executor are not. **Always call `shutdown()`** so the JVM can exit
cleanly.

---

> [📖 Index](README.md) · Previous: [← Extending](extending.md) · Next: [Design Debt →](design-debt.md)
