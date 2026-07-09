# Roadmap

> [📖 Index](README.md) · Previous: [← Design Debt](design-debt.md) · [🇨🇳 中文](roadmap.zh-CN.md)

This roadmap guides x-remoting from its current 0.x preview state toward a
production-ready 1.0 release. Each phase builds on the previous one; items
within a phase can be tackled independently unless noted otherwise.

**Current snapshot** (as of `6e3b5f6`):

| Metric | Value |
|--------|-------|
| Version | 0.0.3-RC2 |
| Source LOC | ~5 300 |
| Tests | 94+ |
| JaCoCo floor | 40% |
| CI matrix | JDK 8 / 11 / 17 / 21 |
| Deps automation | Dependabot (Maven + Actions, weekly) |
| Docs | 9 topics × 2 languages + design-debt audit |

---

## What's already done

These improvements have landed since the design-debt audit:

- [x] Reconnect mechanism rewrite (per-endpoint state machine, backoff + jitter)
- [x] Config classes → immutable builders with validation
- [x] `InetSocketAddress` in public API
- [x] Netty dependency split (`netty-all` → modular)
- [x] Heartbeat `AtomicInteger` race fix
- [x] Misleading thread pool rename
- [x] `CallOptions` → immutable builder
- [x] `Connection` god object triage (`InFlightRequests` + `HeartbeatState`)
- [x] Transport / RPC layer separation (`Protocol` minimal, `impl.*` → `rpc.*`)
- [x] Single-module consolidation (merged `api` / `core` / `all` / `examples`)
- [x] `@AccessForTest` annotation deleted
- [x] `CHANGELOG.md` created

---

## Phase 0 — Open-source governance

> Quick wins. No code changes. Can be done in a day.

- [ ] **`SECURITY.md`** — vulnerability reporting channel (email / GitHub
  advisories). Explicitly document that the default Hessian serializer must not
  be used on untrusted networks.
- [ ] **`CONTRIBUTING.md`** — dev environment setup (JDK 8+, Maven), code style
  (`spring-javaformat:apply`), PR process, commit message conventions.
- [ ] **Issue & PR templates** (`.github/ISSUE_TEMPLATE/`, `.github/pull_request_template.md`):
  bug report, feature request, PR checklist.
- [x] **`CHANGELOG.md`** — created and tracking changes.
- [ ] **`CODE_OF_CONDUCT.md`** — adopt Contributor Covenant or similar.

---

## Phase 1 — API stability (→ 0.1.0)

> Get the public surface right before people depend on it.

### Protocol extension point — make it real

The `Protocol` interface exists but only has one implementation, and
`AbstractMessageHandler` hardcodes handler registration. Fix direction:

- [ ] **Replace `MessageTypeHandler` hierarchy with `MessageDispatcher`** —
  a single `Map<MessageType, BiConsumer<Connection, Message>>` dispatcher.
  Each `Protocol` implementation registers its own message types and handlers.
  Deletes 5 classes (`MessageTypeHandler`, `AbstractMessageHandler`,
  `AbstractRequestMessageTypeHandler`, `HeartbeatRequestMessageTypeHandler`,
  `ResponseMessageTypeHandler`), replaces with 1. See
  [Design Debt § MessageType](design-debt.md#-messagetype--messagetypehandler-family-is-over-inherited).
- [ ] **Make `MessageType` extensible** — change from a fixed enum to an
  interface so custom protocols can define their own types.

### Other API items

- [ ] **`IDGenerator` → per-client `AtomicLong`** — eliminates JVM-wide
  sharing, `Integer` autoboxing, and 2³¹ wrap-around risk.
- [ ] **`RequestHandler<T, R>` SPI cleanup** — single method
  `void handle(T, ResponseObserver<R>)`. Provide `BlockingRequestHandler`
  adapter for sync usage.
- [ ] **`ServerConfig` → builder** — align with the other `*Config` classes.
- [ ] **`Connection.remoteAddress()`** — add `inetRemoteAddress()` that returns
  `InetSocketAddress` directly, or migrate EmbeddedChannel tests.
- [ ] **`RequestApi` review** — either take a raw `String`, or grow into a
  type with version / timeout / metadata.

---

## Phase 2 — Runtime correctness (→ 0.2.0)

> Fix the gaps that bite in production.

- [ ] **Write-path backpressure** — check `Channel.isWritable()` before write;
  fast-fail with a clear exception when the high-water mark is hit.
  See [Design Debt § No write backpressure](design-debt.md#-no-write-backpressure).
- [ ] **Graceful server drain (GOAWAY)** — introduce a `GOAWAY`-equivalent
  message type. Clients receiving it should `cancel(addr)` or back off instead
  of treating the disconnect as a failure.
- [ ] **Serialization SPI** — change `SerializationType` from an enum to an
  open registry. Ship Kryo and/or Protobuf as opt-in modules.
  Document Hessian deserialization risk.
- [ ] **Exception handling consistency** — `AbstractMessageHandler` (or its
  replacement) should log full stack trace at WARN, include exception class name
  in response, and add structured fields (request id, path) for log → trace
  correlation.

---

## Phase 3 — Observability & hardening (→ 0.3.0)

> Make it operable in production environments.

- [ ] **Metrics SPI** — define a `MetricsProvider` interface. Out-of-the-box
  implementations for Micrometer and/or OpenTelemetry. Key metrics:
  - Active connections (gauge, per endpoint)
  - Request latency (histogram)
  - Reconnect attempts / successes / abandonments (counters)
  - Heartbeat failures (counter)
  - In-flight requests (gauge)
- [ ] **Structured logging** — ensure every log line at WARN+ carries request
  id, path, and remote address.
- [ ] **Test coverage → 60%+** — focus on `Connection`, `Call`, reconnect, and
  message dispatch paths.
- [ ] **Performance baseline** — JMH benchmarks for throughput and latency
  (sync call, async call, oneway) to prevent regressions.

---

## Phase 4 — 1.0.0

> Stable release.

- [ ] **API freeze** — no breaking changes within the 1.x line. Public classes
  without `@Deprecated` are the contract.
- [ ] **Migration guide** — document every breaking change from 0.x → 1.0 with
  before/after code.
- [ ] **Release to Maven Central** — publish non-RC artifacts.
- [ ] **Announce** — blog post / README / GitHub release notes.

---

## Housekeeping (do whenever convenient)

These are small, independent improvements that can be folded into any PR:

| Item | Severity | Effort |
|------|----------|--------|
| `Resource<T>` → `Supplier` + `Closeable` | 🟡 | S |
| Document why `Validate` exists (vs. Commons/Guava) | 🟡 | S |
| `IDGenerator.nextRequestId()` return `int` not `Integer` | 🟡 | S |
| Heartbeat through dedicated tiny executor | 🟡 | M |

---

## How to pick up an item

1. Check the issue tracker — if no issue exists, create one and tag it with the
   relevant phase label.
2. Comment on the issue to claim it.
3. Branch from `main`, implement, run `mvn clean install` to verify.
4. Open a PR referencing the issue. The CI matrix must be green.

See [CONTRIBUTING.md](../CONTRIBUTING.md) (coming in Phase 0) for full details.

---

> [📖 Index](README.md) · Previous: [← Design Debt](design-debt.md)
