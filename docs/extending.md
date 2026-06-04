# Extending x-remoting

> [📖 Index](README.md) · Previous: [← Reconnect](reconnect.md) · Next: [FAQ →](faq.md) · [🇨🇳 中文](extending.zh-CN.md)

This page covers the most common extension points.

## Custom `BackoffPolicy`

A `BackoffPolicy` decides how long to wait before the next reconnect attempt.

```java
public interface BackoffPolicy {
    /** Returns delay in nanos; return < 0 to signal "give up". */
    long nextDelayNanos(int attempts);
}
```

Example — decorrelated jitter (AWS pattern):

```java
public class DecorrelatedJitter implements BackoffPolicy {
    private final long baseNanos;
    private final long capNanos;
    private long lastNanos;

    public DecorrelatedJitter(long base, long cap, TimeUnit unit) {
        this.baseNanos = unit.toNanos(base);
        this.capNanos  = unit.toNanos(cap);
        this.lastNanos = baseNanos;
    }

    @Override
    public synchronized long nextDelayNanos(int attempts) {
        long upper = Math.min(capNanos, lastNanos * 3);
        long delay = ThreadLocalRandom.current().nextLong(baseNanos, upper + 1);
        lastNanos = delay;
        return delay;
    }
}

ReconnectConfig cfg = ReconnectConfig.builder()
        .backoffPolicy(new DecorrelatedJitter(100, 30_000, TimeUnit.MILLISECONDS))
        .build();
```

## Custom `ReconnectListener`

Already covered in [Reconnect](reconnect.md). Common patterns:

```java
// Micrometer metrics
reconnector.addListener(new ReconnectListener() {
    @Override public void onScheduled (InetSocketAddress a, int n, long ns) {
        meterRegistry.counter("remoting.reconnect.scheduled", "addr", a.toString()).increment();
    }
    @Override public void onSuccess   (InetSocketAddress a, int n) {
        meterRegistry.counter("remoting.reconnect.success", "addr", a.toString()).increment();
    }
    @Override public void onFailure   (InetSocketAddress a, int n, Throwable t) {
        meterRegistry.counter("remoting.reconnect.failure", "addr", a.toString(), "exc", t.getClass().getSimpleName()).increment();
    }
    @Override public void onAbandoned (InetSocketAddress a, int n, Throwable t) {
        meterRegistry.counter("remoting.reconnect.abandoned", "addr", a.toString()).increment();
        serviceRegistry.evict(a);
    }
});
```

## Custom `ConnectionEventListener`

Get notified of every `CONNECT` / `CLOSE`. Use the optional `executor()` method to
move slow work off the dispatcher:

```java
client.getConnectionManager().connectionEventProcessor()
      .addConnectionEventListener(new ConnectionEventListener() {
          @Override public void onEvent(ConnectionEvent evt, Connection conn) {
              auditLogService.record(evt, conn.remoteAddress());   // slow I/O
          }
          @Override public Executor executor() {
              return auditExecutor;        // run on my own pool
          }
      });
```

If the executor returns null (default), the listener runs inline on the dispatcher
and blocks subsequent events.

## Custom `ConnectionEventProcessor`

If even a single dispatcher thread is too slow, plug in your own executor:

```java
ExecutorService dispatcher = Executors.newFixedThreadPool(4,
        new NamedThreadFactory("my-event-dispatch"));
DefaultConnectionEventProcessor processor = new DefaultConnectionEventProcessor(dispatcher);
```

The caller owns `dispatcher.shutdown()`. (Wiring this into `ClientConnectionManager`
requires constructing the manager directly — the public RPC client doesn't yet expose
this knob.)

## Custom `ConnectionSelectStrategy`

The default strategy is round-robin via an `AtomicInteger` counter. Drop in your own
for weighted selection, sticky routing, etc.

```java
public class StickyByThreadStrategy implements ConnectionSelectStrategy {
    @Override public Connection select(List<Connection> connections) {
        if (connections.isEmpty()) return null;
        int idx = (int) (Thread.currentThread().getId() % connections.size());
        return connections.get(idx);
    }
}
```

Currently the strategy is wired in `AbstractConnectionManager` as a `protected` field
(`connectionSelectStrategy`); subclass to override.

## Custom `Heartbeater`

Implement `Heartbeater` to change how heartbeats are sent or to gate them on app
state. The framework calls `triggerHeartBeat(Connection)` on every
`IdleStateEvent`. The default impl handles fail counting + close-on-threshold; if
you replace it, replicate that logic or you'll leak dead connections.

## Custom `Protocol` (advanced)

`Protocol` is the wire-level façade: it exposes a `MessageCodec`, `MessageFactory`,
`MessageHandler`, and a unique `ProtocolId`. Implementing one means owning:

1. Frame format (length prefix, magic, header layout) — encode/decode in `MessageCodec`
2. Message types (request, response, heartbeat) — `MessageFactory.createXxx(...)`
3. Per-type dispatch — `MessageHandler` and one or more `MessageTypeHandler`

The bundled `RemotingProtocol` in `core` is the reference implementation. If your
needs match RPC, prefer extending it (register your own `RequestHandler`s) rather
than rolling a new protocol from scratch.

> See also [Design Debt](design-debt.md) — the `Protocol` extension point is currently
> more aspirational than complete. The surrounding `Message` / `MessageType`
> hierarchy is shaped for `RemotingProtocol` specifically.

## Sharing a Netty `EventLoopGroup` across multiple clients

`DefaultConnectionFactory` creates its own `EventLoopGroup`. If you run many
clients in the same JVM and want them to share Netty threads, either:

- Pass a shared `Executor` and `Timer` via `ConnectionFactoryConfig` so at least
  callbacks and timeouts are shared (you still get separate worker groups), or
- Use the framework module (`api`) directly and construct `ClientConnectionManager`
  with a custom `ConnectionFactory` that uses your shared `EventLoopGroup`.

This is on the roadmap but not yet a first-class config knob.

---

> [📖 Index](README.md) · Previous: [← Reconnect](reconnect.md) · Next: [FAQ →](faq.md)
