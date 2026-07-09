# Changelog

## [Unreleased] — 2026-07-09

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
- JaCoCo coverage check passes (≥ 0.40 complexity coverage).
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
| `import ...impl.handler.RequestApi` | `import ...rpc.handler.RequestApi` |
| `import ...impl.handler.RequestHandler` | `import ...rpc.handler.RequestHandler` |
| `import ...impl.client.RemotingCallBack` | `import ...rpc.client.RemotingCallBack` |
| `import ...impl.client.RemotingFuture` | `import ...rpc.client.RemotingFuture` |
| `import ...impl.server.RemotingServerConfig` | `import ...rpc.server.RemotingServerConfig` |
| `connection.getInFlightRequests()` | `InFlightRequests.getOrCreate(connection)` (channel attribute) |
| `connection.getHeartbeatState()` | `HeartbeatState.getOrCreate(channel)` (channel attribute) |
| `protocol.getMessageFactory()` | Cast to `RpcProtocol` first, then `.getMessageFactory()` |
