# Changelog

## [Unreleased] — 2026-07-10

### Fix: production readiness hardening

Comprehensive audit and fix of ~35 issues across security, data correctness,
lifecycle management, and robustness.

#### Security

- **`ClassFilter`** — new deserialization guard. All `Class.forName()` calls in
  `RemotingMessageBody` and `DefaultMessageHeaders` now route through
  `ClassFilter.loadClass()`, which blocks known dangerous classes/prefixes
  (`java.lang.Runtime`, `javax.management.*`, `sun.reflect.*`, etc.). Users can
  add custom blocked prefixes via `ClassFilter.addBlockedPrefix()`.
- **`FurySerializer`** — switched to `requireClassRegistration(true)`. Only
  explicitly registered classes can be deserialized. Added
  `FurySerializer.registerClass()` API.

#### Data correctness

- **`RoundRobinConnectionSelectStrategy`** — fixed `Math.abs(Integer.MIN_VALUE)`
  overflow that caused `IndexOutOfBoundsException` in long-running processes.
  Now uses bitmask: `counter & Integer.MAX_VALUE`.
- **`DefaultMessageHeaders.contains()`** — fixed: was calling
  `ConcurrentHashMap.contains()` (≡ `containsValue()`), always returned `false`.
  Changed to `containsKey()`.
- **`InFlightRequests.getOrCreate()`** — race condition on channel attribute
  fixed with `Attribute.setIfAbsent()` (CAS).
- **`InvokeFuture.complete()`** — race between timeout and IO thread fixed with
  `AtomicBoolean` CAS instead of `Validate.isTrue`.
- **`RpcMessageDispatcher`** — added null guard for `InFlightRequests.of()` and
  `Connection` attribute; prevents NPE on edge-case responses.
- **`RemotingMessageDecoder`** — `readShort()` → `readUnsignedShort()` for path
  and header lengths; added negative body-length check; `remainLength` uses
  `long` arithmetic to prevent integer overflow.
- **`DefaultMessageHeaders` deserialize** — `readShort()` → `readUnsignedShort()`
  for key/valueType/value lengths.
- **`RemotingServer.sendGoaway()`** — creates a separate GOAWAY message per
  connection (was sharing one object across concurrent IO threads).

#### Lifecycle & resource management

- **`AbstractLifeCycle.shutdown()`** — now idempotent (no-throw on repeated call).
- **`AbstractServer`** — `EventLoopGroup` creation moved from field initializer
  to `startup()` (no leaked threads if constructor is called but `startup()` is
  not). Startup failure now calls `shutdown()` to clean up sub-components.
  `shutdown()` awaits `EventLoopGroup` termination with `syncUninterruptibly()`.
- **`DefaultConnectionFactory`** — same lazy `EventLoopGroup` pattern; `close()`
  now awaits termination.
- **`ClientConnectionManager`** — shutdown order fixed:
  reconnector → connections → factory (was: connections → factory → reconnector).
- **`RemotingServer`** — GOAWAY writes now await completion (up to 3s) before
  proceeding with shutdown.
- **Default server executor** — changed from unbounded `CachedThreadPool` to
  bounded `FixedThreadPool(cores×2)`.
- **All internal thread pools** — daemon threads by default (IO workers, timers,
  reconnect workers, event dispatchers). Prevents JVM from hanging on shutdown.

#### Robustness

- **`AbstractConnectionManager.close()`** — null `remoteAddress` guard.
- **`ProtocolDecoder`** — closes the channel on decode failure (was only skipping
  bytes, leaving a corrupt stream).
- **`HessianSerializer`** — `ThreadLocal<ByteArrayOutputStream>` removed when
  buffer exceeds 64 KB, preventing memory accumulation.
- **`DefaultMessageHeaders` serialize** — validates field lengths ≤
  `Short.MAX_VALUE`, throws `SerializeException` on overflow.
- **`CallOptions.headers()`** — null validation added.
- **`RemotingMessageBody.deserialize()`** — `ByteBuf` now released in `finally`
  block (was leaked).

#### Tests

- **102 tests** (8 new) — all passing.
- New: `ClassFilterTest` (6 tests) — blocked classes, allowed classes, load
  blocked, load not found, load allowed, custom prefix.
- New: `AbstractLifeCycleTest` (2 tests) — idempotent shutdown, double startup
  throws.
- JaCoCo complexity coverage ≥ 60%.

#### Documentation

- `SECURITY.md` — rewritten with `ClassFilter`, `FurySerializer` class
  registration, and Hessian risk sections.
- `configuration-reference.md` (EN + CN) — server executor default updated
  from "cached pool" to "fixed pool (cores×2)".

#### Known limitations (documented, not fixed)

- Hessian deserializer internally resolves classes, bypassing `ClassFilter`.
  Use Fury with class registration on untrusted networks.
- `FurySerializer` classes must be registered before thread pool initialization.
- `IDGenerator` `AtomicInteger` wraps at 2³¹; collision requires ~2 billion
  concurrent in-flight requests.

---

### Refactor: Serialization SPI extensibility

`SerializationType` converted from a closed enum-like interface to an open
`ConcurrentHashMap` registry (same pattern as `MessageType`). Built-in types
auto-register via `SerializationType.create(byte)`.

- **Hessian typo fix**: `Hession` → `Hessian` globally (class name, constant,
  all references in source + tests + docs).
- **Wire code change**: Hessian code changed from `-1` to `1` (breaking wire
  format change; approved for 0.x pre-release).
- **Fury serializer**: added Apache Fury 0.10.3 as a second built-in serializer
  (`SerializationType.Fury`, code `2`). ThreadLocal-based Fury instances.
- **SerializationManager**: `HashMap` → `ConcurrentHashMap` for thread safety.
- `HessionSerializer.java` renamed to `HessianSerializer.java`.

---

### Fix: structured logging audit

Added structured context fields to WARN+ log lines:
- `RemotingMessageDecoder`: added `remoteAddress` to unsupported MessageType
  warning and decode failure error. Fixed "encode" typo → "decode".
- `RemotingMessageEncoder`: added `remoteAddress` to encode failure error.
- `Responses`: added `id` to serialize failure error.
- `BlockingRequestHandler`: added exception message to error log.

---

### Build: raise JaCoCo floor to 60%

JaCoCo complexity coverage floor raised from 40% to 60% (current coverage
exceeds this). Reflects the improved test suite.

---

### Build: JMH performance benchmarks

Added JMH 1.37 dependency and `RpcBenchmark` class with three benchmarks:
`blockingCall`, `futureCall`, `onewayCall`. Not part of the normal test suite —
run manually via the `main()` method.

---

### Refactor: thread pool naming cleanup

Removed misleading thread name prefixes:
- `DefaultConnectionFactory`: `"RemotingClient-Client-*"` → `"RemotingClient-*"`
  (removed redundant `Client-` segment).
- `DefaultConnectionEventProcessor`: `"RemotingClient-Connection-Event"` →
  `"Connection-Event"` (neutral name since processor is used on both client and
  server sides).

---

### Refactor: Connection.inetRemoteAddress()

Added `Connection.inetRemoteAddress()` that returns `InetSocketAddress` directly,
centralizing the cast from `SocketAddress`. Updated `AbstractConnectionManager`
and `DefaultHeartbeater` to use the new method, fixing a latent type-safety bug
in `DefaultHeartbeater.disabledSocketAddresses.contains()`.

---

### Refactor: MessageType extensibility

`MessageType.valueOf(byte)` changed from a closed `switch` statement to an open
`ConcurrentHashMap` registry. Built-in types (`heartbeatRequest`, `request`,
`response`, `goaway`) are auto-registered via `MessageType.create(byte)`. Custom
protocols can register their own types via `MessageType.register(type)`.

---

### Refactor: ServerConfig → immutable builder

`ServerConfig` converted from mutable `@Data` to immutable builder pattern
(`@Getter`, private constructor, `Builder` inner class, `defaults()` factory).
Validation moved into the constructor (port range, idle timeout constraints).
Empty `RemotingServerConfig` subclass deleted — `RemotingServer` now takes
`ServerConfig` directly.

---

### Governance: SECURITY.md, CONTRIBUTING.md, CODE_OF_CONDUCT.md, templates

Added open-source governance files:
- `SECURITY.md` — vulnerability reporting channel, Hessian deserialization risk.
- `CONTRIBUTING.md` — dev setup, code style, PR process.
- `CODE_OF_CONDUCT.md` — Contributor Covenant v2.1.
- `.github/ISSUE_TEMPLATE/bug_report.md` and `feature_request.md`.
- `.github/pull_request_template.md`.

---

### Docs: delete design-debt and roadmap

All items from `design-debt.md` and `roadmap.md` have been resolved or moved to
the issue tracker. Both files (English + Chinese) deleted. Navigation links in
FAQ, docs index, and root README updated.

---

### Feature: graceful server drain (GOAWAY)

Added `MessageType.goaway` (wire code `2`) and `ResponseStatus.Goaway` (code
`12`). `RemotingServer.shutdown()` now sends a GOAWAY message to all managed
connections before closing. Clients receiving GOAWAY set a channel attribute
(`Connection.GOAWAY`) and close the connection; the reconnector cancels (rather
than retrying) for that address.

New classes / constants: `MessageType.goaway`, `ResponseStatus.Goaway`,
`Connection.GOAWAY` attribute key, `MessageFactory.createGoaway()`,
`ServerConnectionManager.forEachConnection()`.

---

### Refactor: IDGenerator → per-client instance

`IDGenerator` converted from a static utility (JVM-wide `AtomicInteger`) to an
instance class. Each `RemotingClient` and `RemotingServer` creates its own
`IDGenerator`, shared between its `RemotingCall` and `DefaultHeartbeater`.
Eliminates JVM-wide state and `Integer` autoboxing (now returns `int`).

---

### Refactor: delete Resource\<T\> abstraction

`Resource<T>` interface deleted. `DefaultConnectionFactory` and `AbstractServer`
now create executor and timer inline in constructors and close them directly in
shutdown methods. Removes an unnecessary layer of indirection.

---

### Refactor: replace homegrown Validate with Apache Commons Lang3

Replaced `io.github.xinfra.lab.remoting.common.Validate` with
`org.apache.commons.lang3.Validate` (Apache Commons Lang3 3.17.0). The API is
compatible — all call sites updated with a simple import swap. Homegrown
`Validate.java` deleted.

---

### Fix: write-path backpressure in heartbeat

`ConnectionManager.check()` already checks `Channel.isWritable()` before every
RPC write. `DefaultHeartbeater.triggerHeartBeat()` now also checks
`isWritable()` and skips the heartbeat when the channel is overloaded, closing
the last gap in write-path backpressure.

---

### Fix: structured error logging

`RpcMessageDispatcher` and `RemotingRequestMessageHandler` now log full stack
traces with structured fields: request id, message type / path, and remote
address. Enables log → trace correlation in production.

---

### Fix: heartbeat callback uses dedicated executor

`DefaultHeartbeater` now owns a dedicated single-thread daemon executor
(`Heartbeat-Callback`). Heartbeat callbacks use `InvokeCallBack.getExecutor()`
to run on this executor instead of the connection's shared executor, preventing
heartbeat starvation under load.

---

### Refactor: simplify RequestHandler SPI

`RequestHandler<T, R>` reduced from 3 methods (sync `handle`, default
`asyncHandle`, default `getExecutor`) to 1 abstract + 1 default:

- `void handle(T request, ResponseObserver<R> responseObserver)` — the single
  method all handlers implement.
- `default Executor getExecutor()` — optional executor override (unchanged).

New **`BlockingRequestHandler<T, R>`** adapter for synchronous handlers:

- `abstract R handleRequest(T request)` — implement this for sync work.
- `static <T, R> BlockingRequestHandler<T, R> of(Function<T, R> fn)` — one-line
  lambda factory.

Migration: `(req) -> result` → `BlockingRequestHandler.of((req) -> result)`.
Async handlers now implement `RequestHandler` directly with
`(req, observer) -> { ... }`.

---

### Refactor: collapse MessageTypeHandler hierarchy

Replaced the 6-class `MessageTypeHandler` inheritance tree with 2 classes:

- **`RpcMessageDispatcher`** — implements `MessageHandler`, dispatches by
  `MessageType`: heartbeat and response handling are inlined; request handling
  is delegated to `RemotingRequestMessageHandler`.
- **`RemotingRequestMessageHandler`** — dispatches request messages by path
  (`String`). Holds a `ConcurrentHashMap<String, RequestHandler>` internally,
  replacing the separate `RequestHandlerRegistry` class.

Deleted classes: `AbstractMessageHandler`, `MessageTypeHandler`,
`AbstractRequestMessageTypeHandler`, `HeartbeatRequestMessageTypeHandler`,
`ResponseMessageTypeHandler`, `RemotingMessageHandler`,
`RemotingRequestMessageTypeHandler`, `RequestApi`, `RequestHandlerRegistry`.

`RequestApi` wrapper class removed — all public APIs (`blockingCall`,
`futureCall`, `asyncCall`, `oneway`, `registerRequestHandler`) now take
`String path` directly.

---

### Summary

Two major structural refactors applied back-to-back:

1. **API/Core layer separation** — cleaned the boundary between the transport
   framework and the RPC implementation so they are genuinely independent.
2. **Single-module consolidation** — merged the four Maven modules (`api`,
   `core`, `all`, `examples`) into one flat module, eliminating cross-module
   complexity.

Net effect: **169 files changed, +247 / −9,797 lines**.
The project is now a single `jar` artifact (`io.github.x-infra-lab:x-remoting`)
with 113 main source files, 33 test files, and 94 passing tests.

---

### Refactor: API / Core layer separation

Cleaned the boundary between the transport layer and the RPC layer. This was a
prerequisite for the module merge — it ensured the two layers are logically
independent even though they now live in the same module.

#### Transport layer (`io.github.xinfra.lab.remoting.*`)

- **`Connection`** trimmed to a lean wrapper: Channel, Protocol, Executor,
  Timer, close hooks, `closed` flag. RPC-specific state removed.
- **`Protocol`** reduced to minimal interface: `getCodec()` + `getMessageHandler()` +
  `getProtocolId()`. RPC-specific methods (`getMessageFactory()`, typed message
  accessors) removed from this interface.
- **`Message`** changed to a marker interface — no RPC-specific methods.
- **`MessageHandler`** simplified to `void handle(Connection, Object)`.
- **`ConnectionManager`** interface: `check()` → `checkAndInit()` with
  `Protocol` parameter for extensibility.
- `InFlightRequests` and `HeartbeatState` removed from `Connection` fields —
  they now live as Netty channel attributes, registered via close hooks.
- `ConnectionClosedException` moved from transport to RPC layer.

#### RPC layer (`io.github.xinfra.lab.remoting.rpc.*`)

Packages renamed from `impl.*` to `rpc.*`:

| Old package | New package |
|-------------|-------------|
| `impl.client` | `rpc.client` |
| `impl.server` | `rpc.server` |
| `impl.handler` | `rpc.handler` |
| `impl.codec` | `rpc.codec` |
| `impl.message` | `rpc.message` |
| `impl.exception` | `rpc.exception` |
| `impl.heartbeat` | `rpc.heartbeat` |
| `impl` (RemotingProtocol) | `rpc` |

Concepts moved **into** the RPC layer:
- `Call`, `CallOptions`, `InvokeFuture`, `InvokeCallBack`, `InFlightRequests`,
  `IDGenerator` — from `api/client` to `rpc/client`
- `HeartbeatState`, `Heartbeater`, `DefaultHeartbeater` — heartbeat counting
  and close-on-threshold logic now in `rpc/heartbeat` as channel attributes
- `AbstractMessageHandler`, `MessageType`, `MessageTypeHandler`,
  `RequestMessage`, `ResponseMessage`, `MessageFactory`, `Requests`,
  `Responses`, `ResponseStatus` — from `api/message` to `rpc/message`

New interface:
- **`RpcProtocol`** — extends `Protocol`, adds `getMessageFactory()`.
  `RemotingProtocol` implements this sub-interface.

#### Connection heartbeat redesign

- `HeartbeatState` is now a Netty channel attribute (`AttributeKey`) managed by
  the RPC layer, not a field on `Connection`.
- `DefaultHeartbeater` reads/writes `HeartbeatState` via the channel attribute.
- Cleanup is registered via `Connection.addCloseHook()` so the attribute is
  properly cleared on disconnect.

---

### Refactor: single-module consolidation

Merged the four Maven modules into one. The project is now a single-module
Maven build producing one JAR.

#### What changed

- **`pom.xml`**: `<packaging>pom</packaging>` → `<packaging>jar</packaging>`.
  `artifactId` is `x-remoting`. Removed `<modules>` block. Dependencies from
  `api/pom.xml` and `core/pom.xml` merged and deduplicated into the parent.
  Removed `maven-jar-plugin` test-jar configuration (no longer needed).
- **Source layout**: all source from `api/src/` and `core/src/` moved to
  `src/main/java/` and `src/test/java/`. Package names unchanged.
- **`examples/` module** deleted. QuickStart code rewritten as
  `src/test/java/.../quickstart/QuickStartTest.java` — a JUnit 5 test that
  starts a server on port 0, sends a blocking echo call, and asserts the result.
- **Deleted directories**: `api/`, `core/`, `all/`, `examples/` (all poms and
  source trees).

#### What did NOT change

- Package names: `io.github.xinfra.lab.remoting.*` and
  `io.github.xinfra.lab.remoting.rpc.*` are preserved exactly.
- Published artifact coordinates: `io.github.x-infra-lab:x-remoting` (same
  `groupId` and `artifactId` as the old `all` module).
- CI workflows: `mvn clean install` / `mvn deploy` — no changes needed.
- All build plugins: JaCoCo, Spring Java Format, Surefire, Source, Javadoc,
  Compiler, release profile (flatten + GPG + Central Publishing).

---

### Documentation updates

All 22 documentation files in `docs/` reviewed and updated to reflect the
single-module structure:

| File | Changes |
|------|---------|
| `architecture.md` / `.zh-CN.md` | Full rewrite. Module table → Maven coordinate. ASCII diagram labels changed from `x-remoting-api`/`x-remoting-core` to "Transport layer" / "RPC layer (rpc.*)". Key abstractions table: "api" → "transport", "core" → "RPC". |
| `getting-started.md` / `.zh-CN.md` | Import paths updated from `impl.*` to `rpc.*`. Maven coordinate simplified. |
| `rpc-usage.md` / `.zh-CN.md` | "core module" → "RPC layer (`rpc.*` packages)". |
| `configuration-reference.md` / `.zh-CN.md` | Heartbeater description updated for channel-attribute model. |
| `connection-manager.md` / `.zh-CN.md` | Heartbeat section rewritten: `connection.heartbeatFailCnt` → `HeartbeatState` channel attribute. Server-side sections updated. |
| `extending.md` / `.zh-CN.md` | Module references → layer references. `core/rpc/heartbeat` → `rpc.heartbeat` package. `api` layer → transport layer. |
| `faq.md` / `.zh-CN.md` | "api module" → "transport layer". |
| `design-debt.md` / `.zh-CN.md` | Three items marked ✅ Resolved (Connection god object, Protocol pseudo-extensibility, api/core name-only split). Module references → package/layer language throughout. Comparison section updated. |
| `README.md` / `.zh-CN.md` (docs index) | Architecture description: "Module map" → "Package layers". |

`README.md` (project root) — no multi-module references were present; unchanged.

---

### Tests

- All **94 tests** pass (93 existing + 1 new `QuickStartTest`).
- JaCoCo coverage check passes (≥ 0.60 complexity coverage).
- Spring Java Format validation passes.
- Dead test code removed (`DefaultHeartbeaterTest` with commented-out body).

---

### Breaking changes (for downstream code)

If you were depending on x-remoting sub-modules directly:

| Before | After |
|--------|-------|
| `x-remoting-api` / `x-remoting-core` / `x-remoting` (all) | `x-remoting` (single artifact) |
| `import ...impl.client.RemotingClient` | `import ...rpc.client.RemotingClient` |
| `import ...impl.server.RemotingServer` | `import ...rpc.server.RemotingServer` |
| `RequestApi.of("echo")` | `"echo"` (plain `String`) |
| `import ...impl.handler.RequestHandler` | `import ...rpc.handler.RequestHandler` |
| `import ...impl.client.RemotingCallBack` | `import ...rpc.client.RemotingCallBack` |
| `import ...impl.client.RemotingFuture` | `import ...rpc.client.RemotingFuture` |
| `import ...impl.server.RemotingServerConfig` | `import ...server.ServerConfig` (use `ServerConfig.builder()`) |
| `connection.getInFlightRequests()` | `InFlightRequests.getOrCreate(connection)` (channel attribute) |
| `connection.getHeartbeatState()` | `HeartbeatState.getOrCreate(channel)` (channel attribute) |
| `protocol.getMessageFactory()` | Cast to `RpcProtocol` first, then `.getMessageFactory()` |
