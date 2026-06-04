# Architecture

> [📖 Index](README.md) · Previous: [← Getting Started](getting-started.md) · Next: [RPC Usage →](rpc-usage.md) · [🇨🇳 中文](architecture.zh-CN.md)

x-remoting is laid out as two thin layers:

- **api** — the framework (connection / protocol / event / heartbeat / reconnect
  abstractions and their Netty-backed default implementations).
- **core** — a complete RPC built on the framework (handler registration, request id
  generation, four call shapes, message factory). The `all` module is a convenience
  uber-jar that bundles both.

```
                            ┌────────────────────┐
                            │  Your application  │
                            └─────────┬──────────┘
                                      │
   ┌──────────────────────────────────┴──────────────────────────────────┐
   │                                                                     │
   │                          x-remoting-core                            │
   │   RemotingClient / RemotingServer  (blockingCall / futureCall /     │
   │                                     asyncCall / oneway)             │
   │   RequestHandler / RequestApi / RemotingProtocol / Codec            │
   │                                                                     │
   └──────────────────────────────────┬──────────────────────────────────┘
                                      │
   ┌──────────────────────────────────┴──────────────────────────────────┐
   │                                                                     │
   │                          x-remoting-api                             │
   │   ConnectionManager     ConnectionFactory     Protocol              │
   │   Reconnector           Heartbeater           MessageHandler        │
   │   ConnectionEventProcessor / Listener                               │
   │   AbstractServer        ServerConnectionManager                     │
   │                                                                     │
   └──────────────────────────────────┬──────────────────────────────────┘
                                      │
                                ┌─────┴──────┐
                                │   Netty    │
                                └────────────┘
```

## Modules

| Module | Coordinate | Contents |
|--------|------------|----------|
| `api`  | `io.github.x-infra-lab:x-remoting-api` | Framework abstractions + Netty-backed defaults |
| `core` | `io.github.x-infra-lab:x-remoting-core` | RPC implementation (`impl.client`, `impl.server`, `impl.handler`, `impl.codec`, `impl.message`) |
| `all`  | `io.github.x-infra-lab:x-remoting` | Convenience uber-jar that depends on both |

If you only want the framework (to build your own protocol), depend on `api`. If you
want the RPC out of the box, depend on `all` (or `core` directly).

> Heads-up: the `api` / `core` split is currently more aspirational than enforced.
> See [Design Debt](design-debt.md) for the honest take.

## Key abstractions

| Type | Layer | Role |
|------|-------|------|
| `Protocol` | api | Wire-level façade: codec, message factory, message handler |
| `Connection` | api | Wraps a Netty `Channel` + per-connection executor/timer + outstanding `InvokeFuture` map |
| `Connections` | api | Per-address pool of `Connection`s, with safe `add` after `close` |
| `ConnectionFactory` | api | Builds a `Connection` for an address |
| `ConnectionManager` | api | Get-or-connect, disconnect, close — owns the `connectionsMap` |
| `ConnectionEventProcessor` | api | Async fan-out of `CONNECT` / `CLOSE` events to user listeners |
| `Heartbeater` | api | Sends heartbeats, counts failures, closes the link |
| `Reconnector` | api | Per-endpoint reconnect state machine with backoff + listener |
| `Server` / `AbstractServer` | api | Bind, accept, route to manager |
| `RemotingProtocol` | core | Concrete `Protocol` for the bundled RPC |
| `RemotingClient` / `RemotingServer` | core | RPC entry points — what most users touch |
| `RequestHandler` / `RequestApi` / `RequestHandlerRegistry` | core | Server-side dispatch by path |
| `CallOptions` / `RemotingCallBack` / `RemotingFuture` | core | Per-call options and async result types |

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
