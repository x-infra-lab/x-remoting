# Reconnect

> [📖 Index](README.md) · Previous: [← Connection Manager](connection-manager.md) · Next: [Extending →](extending.md) · [🇨🇳 中文](reconnect.zh-CN.md)

x-remoting's reconnector is a **per-endpoint state machine** with backoff, jitter,
attempt limits, and lifecycle events. Operators get observability via
`ReconnectListener`; the framework handles the rest.

## Design goals

- **Per-endpoint isolation** — one slow / unreachable endpoint must not block
  reconnects to other endpoints.
- **Backoff + jitter by default** — avoid retry storms and synchronised client
  thundering herds.
- **Bounded retries** — operators can cap attempts and/or total wait, after which
  the endpoint is *abandoned* and a listener is notified so the application can
  react (e.g. drop it from a load balancer).
- **No global locks on hot paths** — the reconnector must not block Netty I/O
  threads.
- **Single source of truth** — one state per endpoint, observable via the public
  API.

## State machine

Each `InetSocketAddress` tracked by the reconnector owns an `EndpointReconnectTask`.
Its state is one of:

```
                  ┌──────── disable() ────────┐
                  ▼                            │
              DISABLED ◀─── disable() ─── (any non-STOPPED)
                  │                            │
                enable()                       │
                  ▼                            │
     ┌─────►  IDLE  ◀─────── success ─────────┤
     │         │                               │
     │   onUnhealthy()                         │
     │         ▼                               │
     │     SCHEDULED ── timer fire ─►  CONNECTING
     │         │                               │
     │     cancel()              ┌─────────────┴───────────┐
     │         │                 │                         │
     │         ▼              success                    failure
     │       IDLE                │                         │
     │                           │                  attempts++ ?
     │                           ▼                         │
     │                          IDLE              ◀──────┴──────► ABANDONED
     │                                                                │
     └──────────────────── enable() ──────────────────────────────────┘
```

| State        | Meaning                                                                |
|--------------|------------------------------------------------------------------------|
| `IDLE`       | No reconnect activity. Initial and post-success state.                 |
| `SCHEDULED`  | A timer is waiting to fire the next attempt.                           |
| `CONNECTING` | A blocking connect attempt is currently running on a worker thread.    |
| `DISABLED`   | User has paused this endpoint. Pending timers cancelled.               |
| `ABANDONED`  | Max attempts / max duration reached. User must `enable()` to retry.    |
| `STOPPED`    | Reconnector has been shut down.                                        |

State transitions happen under a per-endpoint lock; listener callbacks are invoked
**outside** that lock so user code can't deadlock with reconnector calls.

## When does reconnect trigger?

The reconnector itself is purely reactive — it does not poll for liveness. Attempts
are scheduled when one of the following calls `onUnhealthy(address)`:

1. **`AbstractConnectionManager.close(connection)`** — invoked from Netty's
   `channelInactive` (link dropped, peer reset, heartbeat close, etc.). After the
   connection is removed from its pool, the address is marked unhealthy so the pool
   can be refilled.
2. **Application code** may call `reconnector.onUnhealthy(address)` directly.

Conversely, the following calls `cancel(address)` and clears any pending attempts:

- `ConnectionManager.disconnect(address)` — the user has explicitly torn down the
  endpoint and does not want it back.

## API

```java
public interface Reconnector extends LifeCycle {
    void onUnhealthy(InetSocketAddress address);   // idempotent; ignored if non-IDLE
    void cancel(InetSocketAddress address);        // user disconnected; clear state
    void disable(InetSocketAddress address);       // pause; survives until enable()
    void enable(InetSocketAddress address);        // resume from DISABLED / ABANDONED
    ReconnectState stateOf(InetSocketAddress address);
    void addListener(ReconnectListener listener);
}
```

### Method semantics

- **`onUnhealthy`** schedules a reconnect only when the endpoint is `IDLE`. Calling
  it while a reconnect is already `SCHEDULED` or `CONNECTING` is a safe no-op.
  Endpoints in `DISABLED`, `ABANDONED`, or `STOPPED` are silently ignored — by
  design, so a flurry of `channelInactive` events cannot revive an endpoint the user
  has deliberately turned off.
- **`cancel`** is callable even after the reconnector has been shut down (it never
  throws `IllegalStateException`). Safe to use from cleanup paths.
- **`disable` / `enable`** are sticky. Disable cancels any in-flight timer; enable
  resets attempts and returns to `IDLE` (it does *not* immediately try to reconnect
  — the next `onUnhealthy` triggers a fresh cycle).

## Listeners

```java
public interface ReconnectListener {
    default void onScheduled(InetSocketAddress address, int attempts, long delayNanos) {}
    default void onSuccess  (InetSocketAddress address, int attempts) {}
    default void onFailure  (InetSocketAddress address, int attempts, Throwable cause) {}
    default void onAbandoned(InetSocketAddress address, int attempts, Throwable lastCause) {}
}
```

Register via `reconnector.addListener(...)`. Listeners are invoked sequentially in
registration order, outside the per-endpoint lock; exceptions thrown by a listener
are logged and do not affect other listeners.

Typical uses:

- Emit metrics (`reconnect.attempts`, `reconnect.abandoned`).
- Remove abandoned addresses from a service-discovery cache.
- Page on `onAbandoned` for critical endpoints.

## Configuration

```java
ReconnectConfig cfg = ReconnectConfig.builder()
        .backoffPolicy(ExponentialBackoffWithJitter.defaults())
        .maxAttempts(20)                                  // -1 = unlimited
        .maxTotalDurationNanos(TimeUnit.MINUTES.toNanos(10))  // -1 = unlimited
        .workerThreads(4)
        .build();
```

`build()` validates and throws `IllegalArgumentException` for `backoffPolicy=null`
or `workerThreads <= 0`.

### Backoff policies

| Class                            | Behaviour                                                            |
|----------------------------------|----------------------------------------------------------------------|
| `ExponentialBackoffWithJitter`   | **Default.** `1s → 2s → 4s → … → 30s` capped, with ±50% jitter.      |
| `FixedIntervalBackoff(interval)` | Constant delay between attempts (legacy behaviour).                  |
| `NoReconnectPolicy`              | Returns `-1` — never retries; effectively turns reconnect off.       |

Implement `BackoffPolicy.nextDelayNanos(int attempts)` for your own. Return a
negative value to signal "give up", which transitions the endpoint to `ABANDONED`.
See [Extending](extending.md) for an example.

## Example

```java
ReconnectConfig reconnect = ReconnectConfig.builder()
        .backoffPolicy(new ExponentialBackoffWithJitter(500, 10_000, TimeUnit.MILLISECONDS, 2.0, 0.3))
        .maxAttempts(50)
        .build();

RemotingClientConfig clientConfig = new RemotingClientConfig();
clientConfig.setReconnectConfig(reconnect);
RemotingClient client = new RemotingClient(clientConfig);
client.startup();

client.getConnectionManager().reconnector().addListener(new ReconnectListener() {
    @Override
    public void onAbandoned(InetSocketAddress addr, int attempts, Throwable lastCause) {
        log.error("dropping {} from rotation after {} failures", addr, attempts, lastCause);
        serviceRegistry.evict(addr);
    }
});
```

## Internals

- **Scheduling** uses a Netty `HashedWheelTimer` owned by the reconnector
  (`RemotingClient-Reconnect-Timer`). Timer fires only enqueue a task to the worker
  pool — they never run the blocking `connect()` themselves.
- **Worker pool** is a fixed `ExecutorService`
  (`RemotingClient-Reconnect-Worker-*`, size = `workerThreads`) that runs the
  blocking TCP connect. One slow endpoint occupies one worker thread; other
  endpoints continue to make progress as long as the pool is not saturated.
- **Per-endpoint state machine** uses one `Object` monitor per
  `EndpointReconnectTask`. Concurrent operations on different endpoints never
  contend. Operations on the same endpoint are short (no blocking I/O under the lock
  — `connect()` runs *outside* the lock and the result is reconciled afterwards).
- **Race with `cancel` / `disable`**: while a `connect()` is in flight, if the user
  cancels or disables the endpoint, the reconnector observes the changed state when
  reconciling the outcome and does not reschedule. The new connection (if it
  succeeded before being cancelled) is left in the pool; a subsequent `disconnect()`
  will close it.

## Shutdown semantics

`Reconnector.shutdown()`:

1. Marks every tracked task as `STOPPED` and cancels its pending timer.
2. Initiates `ExecutorService.shutdown()`.
3. Waits up to **5 seconds** for in-flight `connect()` calls to finish; if any are
   still running, calls `shutdownNow()` to interrupt them.
4. Stops the `HashedWheelTimer`.

The owning `ClientConnectionManager` calls `reconnector.shutdown()` *after*
disconnecting all addresses and closing the underlying `ConnectionFactory`.

## Caveats

- **`onUnhealthy` does not block.** The first attempt fires after the policy's
  initial delay (default ~1s). Code that needs a synchronous reconnect should call
  `ConnectionManager.get()` / `connect()` directly.
- **Listeners must be quick.** They run on the reconnect worker thread; a slow
  listener delays the next attempt scheduling and consumes a worker slot.
- **`enable()` is not automatic after `ABANDONED`.** This is intentional —
  recovering an abandoned endpoint should be an explicit decision by the operator or
  by a service-discovery layer.
- **`ServerConnectionManager.reconnector()` returns `null`.** All reconnect call
  sites in `AbstractConnectionManager` null-check it.

---

> [📖 Index](README.md) · Previous: [← Connection Manager](connection-manager.md) · Next: [Extending →](extending.md)
