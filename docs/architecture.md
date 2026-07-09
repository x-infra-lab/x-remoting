# Architecture

> [📖 Index](README.md) · Previous: [← Getting Started](getting-started.md) · Next: [RPC Usage →](rpc-usage.md) · [🇨🇳 中文](architecture.zh-CN.md)

x-remoting is a single-module project (`io.github.x-infra-lab:x-remoting`)
organized into two logical layers via packages:

- **Transport layer** (`io.github.xinfra.lab.remoting.*`) — connection,
  protocol, event, reconnect abstractions and their Netty-backed defaults.
- **RPC layer** (`io.github.xinfra.lab.remoting.rpc.*`) — handler registration,
  request id generation, four call shapes, message factory, heartbeat.

```
                            ┌────────────────────┐
                            │  Your application  │
                            └─────────┬──────────┘
                                      │
   ┌──────────────────────────────────┴──────────────────────────────────┐
   │                                                                     │
   │                        RPC layer (rpc.*)                            │
   │   RemotingClient / RemotingServer  (blockingCall / futureCall /     │
   │                                     asyncCall / oneway)             │
   │   RequestHandler / RequestApi / RemotingProtocol / Codec            │
   │                                                                     │
   └──────────────────────────────────┬──────────────────────────────────┘
                                      │
   ┌──────────────────────────────────┴──────────────────────────────────┐
   │                                                                     │
   │                      Transport layer                                │
   │   ConnectionManager     ConnectionFactory     Protocol              │
   │   Reconnector           MessageHandler        Message (marker)      │
   │   ConnectionEventProcessor / Listener                               │
   │   AbstractServer        ServerConnectionManager                     │
   │                                                                     │
   └──────────────────────────────────┬──────────────────────────────────┘
                                      │
                                ┌─────┴──────┐
                                │   Netty    │
                                └────────────┘
```

## Maven coordinate

```xml
<dependency>
    <groupId>io.github.x-infra-lab</groupId>
    <artifactId>x-remoting</artifactId>
    <version>0.0.3-RC2</version>
</dependency>
```

## Key abstractions

| Type | Layer | Role |
|------|-------|------|
| `Protocol` | transport | Wire-level façade: codec + message handler |
| `Connection` | transport | Wraps a Netty `Channel` + per-connection executor/timer + close hooks |
| `Connections` | transport | Per-address pool of `Connection`s, with safe `add` after `close` |
| `ConnectionFactory` | transport | Builds a `Connection` for an address |
| `ConnectionManager` | transport | Get-or-connect, disconnect, close — owns the `connectionsMap` |
| `ConnectionEventProcessor` | transport | Async fan-out of `CONNECT` / `CLOSE` events to user listeners |
| `Reconnector` | transport | Per-endpoint reconnect state machine with backoff + listener |
| `Server` / `AbstractServer` | transport | Bind, accept, route to manager |
| `RpcProtocol` | rpc | Extends `Protocol` with `getMessageFactory()` |
| `RemotingProtocol` | rpc | Concrete `RpcProtocol` for the bundled RPC |
| `Heartbeater` | rpc | Sends heartbeats, counts failures via `HeartbeatState`, closes the link |
| `InFlightRequests` | rpc | Per-connection outstanding `InvokeFuture` map (channel attribute) |
| `RemotingClient` / `RemotingServer` | rpc | RPC entry points — what most users touch |
| `RequestHandler` / `RequestApi` / `RequestHandlerRegistry` | rpc | Server-side dispatch by path |
| `CallOptions` / `RemotingCallBack` / `RemotingFuture` | rpc | Per-call options and async result types |

## How a request flows

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                  CLIENT                                       │
│                                                                               │
│  RemotingClient.blockingCall(api, request, addr, opts)                        │
│            │                                                                  │
│            ▼                                                                  │
│  RemotingCall ── buildRequestMessage (id, serialize, headers) ──┐             │
│            │                                                    │             │
│            ▼                                                    ▼             │
│  ConnectionManager.get(addr) ────────────────────┐    InvokeFuture tracked    │
│            │  (pool hit, or connect + fill to N) │                            │
│            ▼                                     │                            │
│  Connection ── pipeline.writeAndFlush ───────────┘                            │
│            │                                                                  │
└────────────┼──────────────────────────────────────────────────────────────────┘
             │
             ▼  TCP, Hessian-encoded length-prefixed frame
┌────────────┼──────────────────────────────────────────────────────────────────┐
│            │                       SERVER                                     │
│            ▼                                                                  │
│  Netty pipeline: ProtocolDecoder → ProtocolHandler → RemotingMessageHandler   │
│            │                                                                  │
│            ▼                                                                  │
│  RemotingRequestMessageTypeHandler                                            │
│            │                                                                  │
│            ▼                                                                  │
│  RequestHandlerRegistry.lookup(path) → user RequestHandler.handle(request)    │
│            │                                                                  │
│            ▼                                                                  │
│  ResponseObserver.complete(result) → Responses.sendResponse                   │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
                       │  TCP back
                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  CLIENT pipeline → ResponseMessageTypeHandler → InvokeFuture.complete()      │
│  RemotingFuture.get() / callback fires / blockingCall returns                │
└──────────────────────────────────────────────────────────────────────────────┘
```

## Threading model

| Thread group | Owned by | Default size | Purpose |
|--------------|----------|--------------|---------|
| `RemotingClient-Client-IO-Worker-*` | `DefaultConnectionFactory` | `availableProcessors()` | Netty NIO/Epoll worker loop for outbound channels |
| `RemotingServer-IO-Boss` | `AbstractServer` | 1 | Accept loop |
| `RemotingServer-IO-Worker-*` | `AbstractServer` | `availableProcessors() * 2` | Netty NIO/Epoll worker loop for inbound channels |
| `RemotingClient-Client-Default-Executor-*` | `DefaultConnectionFactory` | `availableProcessors()` | Runs per-connection callbacks (e.g. `RemotingCallBack`) |
| `RemotingClient-Server-Default-Executor-*` | `AbstractServer` | cached pool | Runs server-side `RequestHandler` invocations |
| `RemotingClient-Client-Timer` / `RemotingClient-Server-Timer` | factory/server | 1 | `HashedWheelTimer` for request timeouts |
| `RemotingClient-Connection-Event` | `DefaultConnectionEventProcessor` | 1 | Fans out `CONNECT` / `CLOSE` events to listeners |
| `RemotingClient-Reconnect-Timer` | `DefaultReconnector` | 1 | `HashedWheelTimer` for scheduling reconnect attempts |
| `RemotingClient-Reconnect-Worker-*` | `DefaultReconnector` | 4 (configurable) | Runs blocking `connect()` for reconnect attempts |

There is **no global lock** on the hot paths. `ConnectionManager` uses
`ConcurrentHashMap.compute` and per-endpoint `Connections` monitors; the reconnector
keeps one tiny monitor per endpoint task; listeners can opt into their own executors
so a slow listener doesn't block the dispatcher. See [Connection Manager](connection-manager.md)
for details.

## Public API contract

All endpoint-keyed APIs (`ConnectionManager`, `Reconnector`, `Heartbeater`, server/client
call methods) take and return **`InetSocketAddress`**. The internal channel API still
uses Netty's `SocketAddress`; `Connection.remoteAddress()` returns `SocketAddress`
and is cast at the framework boundary.

All `*Config` classes are immutable and built via fluent builders. `build()` validates
the inputs and throws `IllegalArgumentException` on bad values. See
[Configuration Reference](configuration-reference.md).

---

> [📖 Index](README.md) · Previous: [← Getting Started](getting-started.md) · Next: [RPC Usage →](rpc-usage.md)
