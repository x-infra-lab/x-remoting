# Design Debt

> [📖 Index](README.md) · Previous: [← FAQ](faq.md) · Next: [Roadmap →](roadmap.md) · [🇨🇳 中文](design-debt.zh-CN.md)

This is an honest, opinionated list of architectural debt and design smells that
exist in the codebase as of `35bb2ce`. The intent is to be useful to future
contributors — not to be polite. Items are grouped by where they hurt and tagged
with severity:

- 🔴 — load-bearing architectural problem; "frameworky" claims fall apart here.
- 🟠 — meaningful design smell or footgun; needs cleanup before 1.0.
- 🟡 — polish or hygiene; do whenever convenient.

What's actually good is acknowledged at the end.

## TL;DR

> Architecturally this is an RPC. The code pretends it's a framework. Most of the
> recent improvements (reconnect, lock-free manager, builder configs,
> `InetSocketAddress`) tightened the implementation; they did not fix the layering.

If the intent is "RPC for personal / project use", things are fine. If the intent
is "framework other projects can build on top of", the `api` vs `core` split needs
real teeth, several abstractions need to be deleted (not extended), and the
`Connection` god object needs to be broken up.

---

## Architectural smells

### 🔴 `api` vs `core` is name-only

The `api` module is supposed to be the protocol-agnostic framework; `core` is the
RPC built on it. In practice the line is not respected:

- `api/.../client/Call.java` is the parent of `core/.../RemotingCall`. The
  "framework" already knows about request/response call shapes.
- `api/.../message/AbstractMessageHandler` constructor hardcodes
  `ResponseMessageTypeHandler` and `HeartbeatRequestMessageTypeHandler`.
- `Connection.invokeMap` holds `InvokeFuture<?>` — a request/response concept —
  in the supposedly transport-level `Connection`.
- `Connection.onClose()` walks `invokeMap` and writes `ConnectionClosed`
  responses. That's RPC behaviour leaking into the connection lifecycle.

**Effect:** If you tried to write a second protocol on top of `api` (a pure push
channel, an event stream, a custom protocol over the same connection
infrastructure), you'd inherit a bunch of RPC-shaped scaffolding you don't want.

**Fix direction:** Either delete the pretense and call the project an RPC, or do
the real work — move RPC-specific concepts (`InvokeFuture`, request/response
message types, `Call`) into `core`, leave `api` as bytes-in / bytes-out + lifecycle.

### 🔴 `Connection` is a god object

One class carries:

1. The Netty `Channel`
2. The `Protocol` reference
3. A per-connection `Executor` (for callbacks)
4. A `Timer` (for request timeouts)
5. An `invokeMap` (outstanding RPCs)
6. The heartbeat failure counter
7. The `closed` flag + idempotent close
8. RPC-aware `onClose` cleanup

That's at least four distinct responsibilities glued together. Test infrastructure
betrays it: `@AccessForTest` is sprinkled on protected fields because clean tests
are otherwise impossible.

**Fix direction:** strip to (1)-(4) + (7); move (5) to a client-side
`InFlightRequests`, (6) to a per-Connection heartbeat state object owned by
`Heartbeater`, (8) to whatever cleans up after a connection on the client side.

### 🔴 `Protocol` is pseudo-extensibility

The `Protocol` interface exists, but the codebase has exactly one implementation
(`RemotingProtocol`) and the surrounding types (`Message`, `RequestMessage`,
`ResponseMessage`, `MessageType` enum, `MessageTypeHandler` family) are all
designed around its shape: length-prefixed framing + Hessian payload + a fixed
3-element message type enum.

**Effect:** a "Protocol" extension point that you cannot actually extend without
forking half of `api/.../message`.

**Fix direction:** pick one
- (a) **Delete the interface** — admit there is only one protocol, simplify
  accordingly. This is the YAGNI move.
- (b) **Make it real** — define `Protocol` so it owns its own message hierarchy,
  not just inherits from a shared one. The price is significant rewriting of
  `api/.../message`.

### 🔴 `MessageType` / `MessageTypeHandler` family is over-inherited

The chain is:

```
MessageHandler (iface)
└─ AbstractMessageHandler
   └─ MessageTypeHandler (iface)
      └─ AbstractRequestMessageTypeHandler
         ├─ HeartbeatRequestMessageTypeHandler
         └─ RemotingRequestMessageTypeHandler
      └─ ResponseMessageTypeHandler
```

Six type slots to dispatch on a three-value enum. The whole thing is equivalent
to one `Map<MessageType, BiConsumer<Connection, Message>>` plus three lambdas.

**Fix direction:** collapse to a single dispatcher class; let users register
lambdas / method references for new types.

---

## Public-API footguns

### 🟠 `Connection.remoteAddress()` returns `SocketAddress`, everything else takes `InetSocketAddress`

A pragmatic compromise to keep EmbeddedChannel-based tests passing (their
`remoteAddress()` returns `EmbeddedSocketAddress`, which is not an
`InetSocketAddress`). Every internal caller casts.

**Fix direction:** add `inetRemoteAddress()` that does the cast once, or migrate
the few tests using `EmbeddedChannel` + real `Connection` to either mock the
connection or use a real loopback channel.

### 🟠 `RequestApi.of("path")` adds nothing

It's a `String` wrapper class with no metadata, no validation beyond non-blank,
no enum-like constraint. Costs an allocation per construction and adds zero
type safety (any string is a valid `RequestApi`).

**Fix direction:** either take a raw `String`, or grow `RequestApi` into
something with declared APIs (version, timeout, sealed registry).

### 🟠 `RequestHandler<T, R>` is a confused half-async SPI

```java
R handle(T request);                                // sync
default void asyncHandle(T, ResponseObserver<R>);   // wraps sync by default
default Executor getExecutor();                     // executor override
```

Three methods, one of which (`handle`) you must implement even when you only
want async, and another (`asyncHandle`) you must remember to override when you
*do* want async — otherwise it silently runs your sync `handle` on the
framework's executor.

**Fix direction:** one method `void handle(T, ResponseObserver<R>)`. Provide a
`BlockingRequestHandler<T, R>` adapter that wraps a sync function. Most users
will reach for the blocking adapter; the SPI itself stays single-shape.

### 🟠 `CallOptions` is `@Data` mutable, used per-call

All other `*Config` classes were migrated to immutable + builder; `CallOptions`
was missed. It is the **hottest** of the configs — used on every call — and is
frequently passed as a shared instance, which makes mutation across threads a
real race.

**Fix direction:** same builder treatment.

### 🟠 `IDGenerator` is JVM-wide `AtomicInteger`

- One counter shared by every `RemotingClient` in the JVM (and every
  `RemotingServer` doing reverse calls).
- `Integer` autoboxing on every request — minor but unnecessary on the hot path.
- Wraps to negative after 2³¹ — `Connection.addInvokeFuture` is guarded with
  `putIfAbsent`, but a collision after wrap turns into a thrown
  `IllegalStateException` instead of a valid request.

**Fix direction:** per-`Connection` or per-`Client` `AtomicLong`. Long buys you
~30 million years at 10k QPS.

---

## Code-smell catalogue

### 🟠 `@AccessForTest` everywhere

The annotation is custom but the pattern is "I made this private/protected but
tests need it". 14 occurrences across the codebase. It's an admission that the
classes are hard to test in isolation — i.e., the design is wrong, not that the
test framework is missing.

**Fix direction:** restructure the offenders for testability (small classes,
fewer collaborators, injectable dependencies), or just make the fields
package-private without the annotation.

### 🟠 Thread name `RemotingClient-Server-Default-Executor-*`

A pool inside the **server** is named with `RemotingClient-` prefix. Copy-paste
from `DefaultConnectionFactory` that wasn't renamed when `AbstractServer` was
written. Operations staff staring at jstack will be confused.

**Fix:** trivial — rename. Same for `RemotingClient-Server-Timer`.

### 🟠 `Resource<T>` abstraction is over-engineered

`DefaultConnectionFactory` wraps its lazy-init executor / timer in a custom
`Resource<T>` interface (`get` + `close`). The same can be expressed with
`Supplier<T>` + `Closeable` from the JDK. The custom abstraction adds a layer
of indirection that buys nothing.

**Fix direction:** delete `Resource`, use `Supplier` + explicit close calls.

### 🟠 `Validate` is a homegrown `Preconditions`

`Validate.notNull`, `Validate.isTrue`, etc. — there's already `Apache Commons
Validate` (it's even shipped under that exact name) and Guava `Preconditions`.
Either pick one and remove the local copy, or document why this one exists.

### 🟡 `IDGenerator.nextRequestId()` returns `Integer` not `int`

Forced boxing on every call. The boxed value is then assigned to an `int`
field via auto-unbox. Pure waste.

---

## Runtime / operational debt

### 🔴 Hessian default + no security stance

`SerializationType.Hession` (typo intact) is the default serializer. Hessian
has had multiple deserialization RCE CVEs over the years; the upstream
`com.caucho:hessian` has not seen a release since 2022.

There is **no `SECURITY.md`**, no documented threat model, no allow/deny list
on deserialized types.

**Fix direction (short term):** add `SECURITY.md` with vulnerability reporting
channel; document that the default serializer must not be used on untrusted
networks. **(Long term):** make `SerializationType` an SPI; ship Kryo and/or
Protobuf as first-class options.

### 🟠 No write backpressure

`Channel.isWritable()` is checked once in `ConnectionManager.check()`. After
that, `Call.writeAndFlush` happily writes regardless of the high-water mark.
A bursty producer + slow consumer = unbounded outbound buffer growth = OOM.

**Fix direction:** check `isWritable()` before write; either block (with
timeout) or fail-fast with a clear exception when not writable.

### 🟠 `AbstractMessageHandler` catches `Exception` and stuffs it into responses

Useful for "don't crash the channel on a bad handler", but it also **buries the
stack trace one level deeper** in production debugging. Worse, it doesn't catch
`Error`, so an OOM in a handler is propagated, but a `RuntimeException` is not.
The split is inconsistent.

**Fix direction:** log the full stack trace at WARN at the catch site, include
exception class name in the response (already partly done), and add structured
fields (request id, path) so log → trace correlation is easy.

### 🟠 No "server-side graceful drain" signal

When a server is shutting down, accepted connections are closed. From the
client's perspective this looks identical to a network failure, so the
reconnector immediately retries — straight back to the draining server. There's
no concept like HTTP/2 GOAWAY.

**Fix direction:** introduce a `GOAWAY`-equivalent message type. On receipt,
clients should `cancel(addr)` (or back off harder) instead of treating it as an
ordinary disconnect.

### 🟡 Heartbeat traffic shares per-connection executor

`Heartbeater` builds a real `RequestMessage`, calls through `Call.asyncCall`,
allocates an `InvokeFuture`, etc. Heartbeat callbacks run on the same per-
connection executor as user RPC callbacks. Under load, heartbeats could be
starved, leading to false "connection unhealthy" decisions.

**Fix direction:** route heartbeats through a dedicated tiny executor, or skip
the `InvokeFuture` machinery for heartbeats specifically.

---

## Comparison to peer projects

For perspective:

- **vs Bolt (alipay/sofa-bolt)** — Bolt's `UserProcessor` abstraction, address
  parser, protocol manager, and connection event API are more complete. Its
  `Protocol` extension point is real — multiple protocol versions coexist.
- **vs Dubbo Remoting** — Dubbo's transport layer is genuinely swappable
  (Netty / Mina / Grizzly historically). The codec / transport / exchange
  layering is explicit. x-remoting's "api / core" split aspires to this but
  isn't there.
- **vs gRPC-Java** — Different problem space (HTTP/2 multiplexing,
  bi-directional streams, flow control). Not a fair comparison; included only
  to note that if HTTP/2-style features ever become a requirement, the current
  one-request-one-response + long-lived pool model can't grow into it
  incrementally.

The takeaway: x-remoting is currently in roughly the same place Bolt was around
its 0.x days. Bolt's progress from there came from sharpening the abstractions,
not by adding features on top of the current ones.

---

## What's actually good

- **Reconnect mechanism (post-rewrite)** — per-endpoint state machine, backoff
  + jitter, listener-based observability, bounded retries. This is the best
  part of the codebase.
- **Lock-free `AbstractConnectionManager`** — `ConcurrentHashMap.compute` +
  two-arg `remove` + per-`Connections` monitor. Sound design, NIO threads no
  longer contend with slow connects on other addresses.
- **`Connections.closed` flag + safe `add()`** — closes the disconnect-vs-add
  race window cleanly.
- **`InetSocketAddress` in public API** — removed casts at call sites, made
  TCP-only explicit.
- **Config builders + validation** — caught real misconfigurations at startup
  (negative timeouts, zero pool size) that previously slipped to runtime.
- **Netty dependency split + Dependabot + JDK matrix CI** — basic hygiene
  is now in place; CVE-tracking and JDK-compat regressions are caught
  automatically.
- **Documentation depth (`docs/`, `wiki/`)** — disproportionately good for the
  size of the codebase. Most 1k-LOC projects don't have this.

---

## Suggested order of repayment

If someone wants to start paying this debt down:

1. **Triage `Connection`** — extract `InFlightRequests` to client side. Single
   biggest win for testability and for "framework" credibility.
2. **Delete or replace `Protocol`** — pick a side. Don't leave a fake
   extension point.
3. **Rename misleading thread pools** — 5 minutes, immediate ops win.
4. **`SECURITY.md` + Hessian risk doc** — minimum security responsibility.
5. **`CallOptions` builder + `IDGenerator` per-client `AtomicLong`** — the
   remaining "I would have fixed this when I had time" items.
6. **Real `Protocol` extensibility OR delete `MessageType` hierarchy** — pick
   one direction, commit to it.
7. **Backpressure on writes** — the highest-impact runtime correctness gap.

---

> [📖 Index](README.md) · Previous: [← FAQ](faq.md) · Next: [Roadmap →](roadmap.md)
