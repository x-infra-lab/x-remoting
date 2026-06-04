# Connection Manager

> [📖 Index](README.md) · Previous: [← Configuration Reference](configuration-reference.md) · Next: [Reconnect →](reconnect.md) · [🇨🇳 中文](connection-manager.zh-CN.md)

`ConnectionManager` is the central abstraction that owns and reuses TCP connections.
On the client it keeps a pool of warm connections per remote address; on the server
it indexes connections accepted from peers.

## Pool layout

```
ClientConnectionManager
   └── connectionsMap : ConcurrentHashMap<InetSocketAddress, Connections>
         ├── 10.0.0.1:8080 → Connections [Conn1, Conn2, Conn3]   (size = N)
         ├── 10.0.0.2:8080 → Connections [Conn4, Conn5, Conn6]
         └── …
```

- The map is a `ConcurrentHashMap`; pool size per address is
  `ConnectionManagerConfig.connectionNumPerEndpoint` (default 1).
- Each `Connections` holds a `CopyOnWriteArrayList<Connection>` plus a `volatile
  boolean closed` flag — after `close()` it rejects further `add()`s by immediately
  closing the offered connection, so disconnect/add races don't leak channels.
- A `ConnectionSelectStrategy` picks one of the live connections. Default is
  `RoundRobinConnectionSelectStrategy`.

## Connection internals

`Connection` wraps a Netty `Channel` plus:

- `Protocol` — codecs and message factory for this channel
- `Executor` — where user callbacks run (defaults to a pool inside `DefaultConnectionFactory`)
- `Timer` — `HashedWheelTimer` for request timeouts
- `invokeMap : ConcurrentHashMap<Integer, InvokeFuture<?>>` — outstanding RPCs by request id
- `closed : AtomicBoolean` — idempotent `close()`; pending `InvokeFuture`s complete with `ConnectionClosed`

The `Connection` constructor fires `ConnectionEvent.CONNECT` on the pipeline.

## End-to-end flows

### `get(addr)` — happy path

```
get(addr)
  ├── connectionsMap.get(addr)
  │     ├── present  → connections.get() → strategy.select(snapshot) → return Connection
  │     └── absent   → fall through
  └── connect(addr)
        ├── connectionsMap.computeIfAbsent(addr, new Connections(...))
        ├── synchronized (connections) {
        │     while (connections.size() < N) {
        │       if (connections.isClosed()) throw RemotingException
        │       conn = connectionFactory.create(addr)
        │       connections.add(conn)
        │     }
        │   }
        └── connections.get()
```

`DefaultConnectionFactory.create(addr)` does `bootstrap.connect(addr)` then waits on
the future with `await(connectTimeout + 100ms)`. **Interrupts are honored** — the
in-flight connect is cancelled and `RemotingException` is thrown.

### `close(connection)` — passive teardown

Fires from `ConnectionEventHandler.channelInactive` (link drop, peer reset, idle
close, heartbeat-driven close, exception) and from `check()` when liveness fails.

```
ConnectionManager.close(conn):
   ├── connections = connectionsMap.get(addr)
   ├── if connections == null  → conn.close(); return
   ├── connections.invalidate(conn)  → conn.close() + COW remove
   │     ├── true  → reconnector.onUnhealthy(addr)
   │     └── false → no-op
   └── if connections.isEmpty()  → connectionsMap.remove(addr, connections)
```

The two-arg `remove(addr, connections)` only deletes if the mapping still points to
the same `Connections` — guards against a concurrent `add()` that recreated the pool.

### `disconnect(addr)` — active teardown

```
ConnectionManager.disconnect(addr):
   ├── reconnector.cancel(addr)                    (stop any pending reconnect)
   ├── connections = connectionsMap.remove(addr)   (atomic detach)
   └── if connections != null: connections.close() (mark closed + close every conn)
```

## Concurrency model

There is no global `synchronized` on the hot paths. Per-operation locks:

| Operation        | Locks held                                            |
|------------------|-------------------------------------------------------|
| `get(addr)`      | None on the hot path. May fall through to `connect`.  |
| `connect(addr)`  | `synchronized (connections)` during the fill loop only. Per-address. |
| `close(conn)`    | None at the manager. `Connections.invalidate` is COW-based. |
| `disconnect(addr)` | None at the manager. `Connections.close` flips a volatile flag. |
| `add(conn)`      | `ConcurrentHashMap` bucket lock during `compute`. Short. |

NIO threads invoking `channelInactive → close(conn)` never contend with a slow
`connect()` on a different address.

## Connection events

`ConnectionEvent` has two values: `CONNECT` (fired from the `Connection` constructor)
and `CLOSE` (fired from `channelInactive`). They're forwarded to
`ConnectionEventProcessor.handleEvent(event, connection)` and asynchronously
dispatched to every registered `ConnectionEventListener`.

```java
client.getConnectionManager().connectionEventProcessor()
      .addConnectionEventListener(new ConnectionEventListener() {
          @Override public void onEvent(ConnectionEvent evt, Connection conn) {
              log.info("{} {}", evt, conn.remoteAddress());
          }
      });
```

### Dispatcher executor

By default, every event is dispatched on a single `RemotingClient-Connection-Event`
thread. A slow listener will delay subsequent events. Two escape hatches:

- **Per-listener executor.** Listeners that need to do I/O or other slow work can
  override `executor()`:
  ```java
  new ConnectionEventListener() {
      @Override public void onEvent(...) { /* slow work */ }
      @Override public Executor executor() { return myPool; }
  }
  ```
- **Custom dispatcher.** Construct `DefaultConnectionEventProcessor(myExecutor)` to
  replace the default single-thread dispatcher entirely. The caller owns the
  executor's lifecycle.

## Heartbeats

The heartbeat is driven by Netty's `IdleStateHandler` (installed when
`ConnectionFactoryConfig.idleSwitch=true`, default). When an idle event fires,
`ProtocolHeartBeatHandler` calls `Heartbeater.triggerHeartBeat(connection)`.

`DefaultHeartbeater` sends a heartbeat `RequestMessage` over the connection:

- **Success** → `connection.heartbeatFailCnt` reset to 0
- **Failure** → `heartbeatFailCnt` incremented (atomically)
- **`heartbeatFailCnt >= heartbeatMaxFailCount`** (default 3) → close the connection,
  which triggers `channelInactive → close(conn) → reconnector.onUnhealthy(addr)`

Heartbeat can be paused per-`Connection` or per-`InetSocketAddress`:

```java
client.getConnectionManager().heartbeater().disableHeartBeat(connection);
client.getConnectionManager().heartbeater().disableHeartBeat(address);
```

Both blocklists are stored in `ConcurrentHashMap.newKeySet()` so they're safe to
mutate from any thread.

## Lifecycle

```
startup()
  └── AbstractConnectionManager.startup()
        ├── super.startup()                        (started = true)
        └── connectionEventProcessor.startup()     (starts dispatcher)
  └── ClientConnectionManager.startup() also:
        └── reconnector.startup()                  (starts HashedWheelTimer + worker pool)

shutdown()
  └── ClientConnectionManager.shutdown()
        ├── super.shutdown()                       (disconnects every address)
        ├── connectionFactory.close()              (shuts down Netty EventLoopGroup)
        └── reconnector.shutdown()                 (cancels timers, drains workers)
```

`AbstractLifeCycle.shutdown()` uses a CAS-protected flag, so a duplicate `shutdown()`
call throws `IllegalStateException`.

## Server-side specialisation

`ServerConnectionManager` reuses the base for `add`, `close`, `disconnect`, `get`,
and `check`, but:

- `connect(addr)` throws `UnsupportedOperationException` — the server does not dial.
- `reconnector()` and `heartbeater()` return `null` — the server doesn't retry or
  ping clients. The abstract base null-checks before every call.

The same `ConnectionEventHandler` runs on the server pipeline, so registered
listeners receive `CONNECT` / `CLOSE` for every accepted channel.

---

> [📖 Index](README.md) · Previous: [← Configuration Reference](configuration-reference.md) · Next: [Reconnect →](reconnect.md)
