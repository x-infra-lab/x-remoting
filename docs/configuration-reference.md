# Configuration Reference

> [📖 Index](README.md) · Previous: [← RPC Usage](rpc-usage.md) · Next: [Connection Manager →](connection-manager.md) · [🇨🇳 中文](configuration-reference.zh-CN.md)

Every tuning knob in x-remoting lives in one of five config classes. All of them are
**immutable**, built via a fluent builder, and validated at `build()` time.

```java
ConnectionFactoryConfig factory   = ConnectionFactoryConfig.builder()....build();
ConnectionManagerConfig manager   = ConnectionManagerConfig.builder()....build();
ReconnectConfig         reconnect = ReconnectConfig.builder()....build();

RemotingClientConfig client = new RemotingClientConfig();
client.setConnectionFactoryConfig(factory);
client.setConnectionManagerConfig(manager);
client.setReconnectConfig(reconnect);

RemotingServerConfig server = new RemotingServerConfig();
server.setPort(8989);
```

## `ConnectionFactoryConfig`

Controls how outbound connections are created.

| Field                     | Default | Effect                                                      |
|---------------------------|---------|-------------------------------------------------------------|
| `idleSwitch`              | `true`  | Install `IdleStateHandler` so the heartbeat runs            |
| `idleReaderTimeout` (ms)  | 15000   | Read idle threshold                                         |
| `idleWriterTimeout` (ms)  | 15000   | Write idle threshold                                        |
| `idleAllTimeout` (ms)     | 15000   | All-idle threshold (read **and** write)                     |
| `connectTimeout` (ms)     | 1000    | Netty `CONNECT_TIMEOUT_MILLIS` + the await ceiling          |
| `useFlushConsolidation`   | `false` | Opt-in to Netty's `FlushConsolidationHandler` for throughput |
| `executor`                | `null`  | Shared `ExecutorService` for `Connection` callbacks (caller owns lifecycle) |
| `timer`                   | `null`  | Shared `Timer` for request timeouts (caller owns lifecycle) |

```java
ConnectionFactoryConfig factory = ConnectionFactoryConfig.builder()
        .connectTimeout(2000)
        .idleAllTimeout(30_000)
        .useFlushConsolidation(true)
        .build();
```

**Validation:** `connectTimeout > 0`; if `idleSwitch=true`, at least one of the three
idle timeouts must be `> 0`.

## `ConnectionManagerConfig`

Controls the pool size.

| Field                       | Default | Effect                  |
|-----------------------------|---------|-------------------------|
| `connectionNumPerEndpoint`  | 1       | Pool size per address   |

```java
ConnectionManagerConfig pool = ConnectionManagerConfig.builder()
        .connectionNumPerEndpoint(4)
        .build();
```

**Validation:** `connectionNumPerEndpoint > 0`.

## `ReconnectConfig`

Controls the reconnect state machine. See [Reconnect](reconnect.md) for the full model.

| Field                    | Default                                  | Effect                                                |
|--------------------------|------------------------------------------|-------------------------------------------------------|
| `backoffPolicy`          | `ExponentialBackoffWithJitter.defaults()` | Delay between attempts (1s → 2s → 4s → 30s, ±50% jitter) |
| `maxAttempts`            | `-1` (unlimited)                         | Stop after this many failed attempts                  |
| `maxTotalDurationNanos`  | `-1` (unlimited)                         | Stop after this much cumulative wall time             |
| `workerThreads`          | 4                                        | Pool for the blocking `connect()` work                |

```java
ReconnectConfig reconnect = ReconnectConfig.builder()
        .backoffPolicy(new ExponentialBackoffWithJitter(500, 10_000, TimeUnit.MILLISECONDS, 2.0, 0.3))
        .maxAttempts(50)
        .maxTotalDurationNanos(TimeUnit.MINUTES.toNanos(10))
        .workerThreads(8)
        .build();
```

**Validation:** `backoffPolicy != null`, `workerThreads > 0`.

### Built-in `BackoffPolicy`s

| Class                          | Behaviour                                                  |
|--------------------------------|------------------------------------------------------------|
| `ExponentialBackoffWithJitter` | **Default.** Exponential with configurable initial/max/multiplier/jitter |
| `FixedIntervalBackoff`         | Constant delay between attempts                            |
| `NoReconnectPolicy`            | Returns `-1` — never retries (effectively disables reconnect) |

Roll your own by implementing `BackoffPolicy.nextDelayNanos(int attempts)`. Return a
negative value to signal "give up". See [Extending](extending.md) for an example.

## `RemotingClientConfig`

A mutable container that glues the three configs above onto a `RemotingClient`. It's
still mutable (Lombok `@Data`) because it's a transient wrapper used once at
construction.

| Field                     | Default | Effect                                       |
|---------------------------|---------|----------------------------------------------|
| `connectionFactoryConfig` | `null` → `ConnectionFactoryConfig.defaults()` | Passed to `ClientConnectionManager` |
| `connectionManagerConfig` | `null` → `ConnectionManagerConfig.defaults()` | Passed to `ClientConnectionManager` |
| `reconnectConfig`         | `null` → `ReconnectConfig.defaults()`         | Passed to `ClientConnectionManager` |

## `RemotingServerConfig` (extends `ServerConfig`)

| Field                | Default | Effect                                              |
|----------------------|---------|-----------------------------------------------------|
| `hostName`           | `null` (bind to all interfaces) | Bind host                  |
| `port`               | 0 (random) | Bind port                                       |
| `manageConnection`   | `false` | Retain accepted connections so the server can call back |
| `idleSwitch`         | `true`  | Install `IdleStateHandler`                          |
| `idleReaderTimeout` (ms) | 0 | Read idle threshold (0 disables)                      |
| `idleWriterTimeout` (ms) | 0 | Write idle threshold (0 disables)                     |
| `idleAllTimeout` (ms)    | 90000 | All-idle threshold                                  |
| `serializationType`  | `Hession` | Default serializer for response messages          |
| `executor`           | `null` → cached pool | Where handlers run                     |
| `timer`              | `null` → owned `HashedWheelTimer` | Where server-side timeouts schedule |

```java
RemotingServerConfig server = new RemotingServerConfig();
server.setPort(8989);
server.setManageConnection(true);
server.setIdleAllTimeout(60_000);
```

`ServerConfig` is still `@Data` (mutable + setters) for backward compatibility — it
predates the builder refactor. It may move to a builder in a future release.

## `CallOptions` (per-call)

Immutable, constructed via `CallOptions.builder()...build()` or `CallOptions.defaults()`.
Passed on each call:

| Field                | Default                  | Effect                                  |
|----------------------|--------------------------|-----------------------------------------|
| `timeoutMills`       | 3000                     | Per-call timeout                        |
| `serializationType`  | `Hession`                | Per-call serializer override            |
| `headers`            | empty `DefaultMessageHeaders` | Custom message headers              |

---

> [📖 Index](README.md) · Previous: [← RPC Usage](rpc-usage.md) · Next: [Connection Manager →](connection-manager.md)
