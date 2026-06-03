# ConnectionManager

> 中文版：[connection-manager.zh-CN.md](./connection-manager.zh-CN.md)

`ConnectionManager` is x-remoting's central abstraction for owning and reusing
TCP connections. On the client side it keeps a pool of warm connections per remote
address; on the server side it indexes connections accepted from peers. All the
machinery for building connections, watching their health, reacting to failures, and
firing lifecycle events sits behind this one interface.

## Type map

```
                                 ConnectionManager  (interface, extends LifeCycle)
                                          ▲
                                          │
                          ┌───────────────┴───────────────┐
                          │                               │
              AbstractConnectionManager           (shared base)
                          │
                ┌─────────┴────────────┐
                │                      │
   ClientConnectionManager       ServerConnectionManager
   (active dialer, owns          (registers accepted
    Reconnector + Heartbeater)    channels; no dialing)
```

Supporting collaborators:

| Type                          | Role                                                      |
|-------------------------------|-----------------------------------------------------------|
| `Connection`                  | Wrapper around a Netty `Channel` + protocol + executor + timer + outstanding `InvokeFuture` map. |
| `Connections`                 | Per-address pool of `Connection` objects with a closed flag and safe `add()`. |
| `ConnectionFactory`           | Builds a `Connection` to a target address.                |
| `DefaultConnectionFactory`    | Netty `Bootstrap`-based implementation.                   |
| `ConnectionFactoryConfig`     | Idle timeouts, connect timeout, optional shared `Executor`/`Timer`. |
| `ConnectionManagerConfig`     | Pool sizing (`connectionNumPerEndpoint`).                 |
| `ConnectionSelectStrategy`    | Picks one `Connection` from a pool (default: round robin). |
| `ConnectionEventProcessor`    | Async fan-out of `CONNECT`/`CLOSE` events to listeners.   |
| `ConnectionEventListener`     | User callback for `CONNECT`/`CLOSE`.                      |
| `ConnectionEventHandler`      | Netty handler that bridges `channelInactive` /`exceptionCaught` to the manager and fires events. |
| `Heartbeater`                 | Triggered by Netty `IdleStateEvent`; sends heartbeat request, counts failures, closes channel after N failures. |
| `Reconnector`                 | Per-endpoint reconnect state machine. See [reconnect.md](./reconnect.md). |

## ConnectionManager interface

```java
public interface ConnectionManager extends LifeCycle {
    Connection connect(InetSocketAddress addr) throws RemotingException;   // build pool, return one
    void       disconnect(InetSocketAddress addr);                          // tear pool down
    Connection get(InetSocketAddress addr) throws RemotingException;        // get-or-connect
    void       check(Connection conn) throws RemotingException;         // liveness probe
    void       close(Connection conn);                                  // remove single conn
    void       add(Connection conn);                                    // register a conn (server)
    Reconnector              reconnector();                             // may be null on server
    ConnectionEventProcessor connectionEventProcessor();
    Heartbeater              heartbeater();                             // may be null on server
}
```

## Pool layout

```
ClientConnectionManager
   └── connectionsMap : ConcurrentHashMap<InetSocketAddress, Connections>
         ├── 10.0.0.1:8080 → Connections [Conn1, Conn2, Conn3]   (size = N)
         ├── 10.0.0.2:8080 → Connections [Conn4, Conn5, Conn6]
         └── …
```

- `connectionsMap` is a `ConcurrentHashMap`; the bucket key is the remote
  `SocketAddress`.
- Each `Connections` holds a `CopyOnWriteArrayList<Connection>` plus a `volatile
  boolean closed` flag. After `close()` it rejects subsequent `add()`s (the offered
  connection is closed instead of leaked).
- Pool size per address is `ConnectionManagerConfig.connectionNumPerEndpoint`
  (default `1`).
- A `ConnectionSelectStrategy` chooses among the live connections; the default
  `RoundRobinConnectionSelectStrategy` uses an `AtomicInteger` counter.

## Connection internals

`Connection` wraps a single Netty `Channel` and carries:

- `Protocol` — codecs and message factory used over this channel.
- `Executor` — where user-space callbacks (e.g. `InvokeCallBack`) run; defaults to a
  shared pool inside `DefaultConnectionFactory`.
- `Timer` — `HashedWheelTimer` used to time out in-flight requests; also shared.
- `invokeMap : ConcurrentHashMap<Integer, InvokeFuture<?>>` — outstanding RPCs keyed
  by request id.
- `closed : AtomicBoolean` — `close()` is idempotent. When closed, all outstanding
  `InvokeFuture`s are completed with `ConnectionClosed` status.

The newly constructed `Connection` immediately fires `ConnectionEvent.CONNECT` on the
channel pipeline.

## Lifecycle

```
startup()
  └── AbstractConnectionManager.startup()
        ├── super.startup()                        (flips started=true)
        └── connectionEventProcessor.startup()     (starts event dispatch thread)
  └── ClientConnectionManager.startup() also:
        └── reconnector.startup()                  (starts HashedWheelTimer + worker pool)

shutdown()
  └── ClientConnectionManager.shutdown()
        ├── super.shutdown()                       (disconnects every address)
        ├── connectionFactory.close()              (shuts down Netty EventLoopGroup)
        └── reconnector.shutdown()                 (cancels timers, drains workers)
```

`AbstractLifeCycle.shutdown()` uses a CAS-protected flag, so a duplicate `shutdown()`
throws `IllegalStateException`.

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
the future with `await(connectTimeout + 100ms)`. Interrupts are honored (the in-flight
connect is cancelled and `RemotingException` is thrown). The constructed `Connection`
ends up on the channel as the `CONNECTION` attribute and fires `ConnectionEvent.CONNECT`
through the pipeline.

### `close(connection)` — passive teardown

This is called from `ConnectionEventHandler.channelInactive` (which fires on link
drop, peer reset, idle close, heartbeat-driven close, exception, etc.) and from
`check()` when liveness fails.

```
NIO thread → channelInactive
   └── if (connectionManager.isStarted()) connectionManager.close(conn)
   └── userEventTriggered(ConnectionEvent.CLOSE)

ConnectionManager.close(conn):
   ├── connections = connectionsMap.get(addr)
   ├── if connections == null  → conn.close(); return
   ├── connections.invalidate(conn)  → conn.close() + COW remove
   │     ├── true  → reconnector.onUnhealthy(addr)   (see reconnect.md)
   │     └── false → no-op
   └── if connections.isEmpty()  → connectionsMap.remove(addr, connections)
```

The `remove(addr, connections)` form only deletes the entry if it still points to
the same `Connections` object — guards against a concurrent `add()` that recreated
the pool.

### `disconnect(addr)` — active teardown

User explicitly says "I am done with this endpoint":

```
ConnectionManager.disconnect(addr):
   ├── reconnector.cancel(addr)                    (stop any pending reconnect)
   ├── connections = connectionsMap.remove(addr)   (atomic detach)
   └── if connections != null: connections.close() (mark closed + close every conn)
```

If a concurrent `add()` slipped in after `remove()`, its target `Connections` is the
**new** one in the map; the old one is closed cleanly. Conversely if the `add()`
landed inside the old `Connections` after `close()` marked it, `Connections.add()`
closes the offered connection instead of leaking it.

### `add(connection)` — server-side registration

```
ConnectionManager.add(conn):
   └── connectionsMap.compute(addr, (k, existing) -> {
         Connections cs = (existing != null) ? existing : new Connections(strategy);
         cs.add(conn);
         return cs;
       });
```

Atomic create-or-append against `connectionsMap`, plus the `closed`-aware `add()` on
`Connections` itself.

## Concurrency model

The reconnect rewrite removed every global `synchronized` from `AbstractConnectionManager`:

| Operation        | Lock(s) held                                          |
|------------------|-------------------------------------------------------|
| `get(addr)`      | None on the hot path. May fall through to `connect`.  |
| `connect(addr)`  | `synchronized (connections)` during the fill loop. Per-address only. |
| `close(conn)`    | None at the manager. `Connections.invalidate` is COW-based. |
| `disconnect(addr)` | None at the manager. `Connections.close` flips a volatile flag. |
| `add(conn)`      | `ConcurrentHashMap` bucket lock during `compute`. Short. |

NIO threads invoking `channelInactive → close(conn)` never contend with a slow
`connect()` on another address.

## ConnectionEvent fan-out

`ConnectionEvent` enum has two values: `CONNECT` (fired in the `Connection`
constructor) and `CLOSE` (fired in `ConnectionEventHandler.channelInactive`). They are
forwarded by `ConnectionEventHandler.userEventTriggered` to
`ConnectionEventProcessor.handleEvent(event, connection)`.

`DefaultConnectionEventProcessor` enqueues every event into an unbounded
`LinkedBlockingQueue` and a single background thread
(`RemotingClient-Connection-Event`) drains the queue and dispatches to each
registered `ConnectionEventListener`. Listener exceptions are caught and logged so
one bad listener cannot affect the others. Event delivery is asynchronous and ordered
per-process (single dispatcher thread).

## Heartbeats

Heartbeats are driven by Netty's `IdleStateHandler` (installed via
`ConnectionFactoryConfig.idleSwitch / idleReader / idleWriter / idleAll`, all
defaulting to 15 s). When an idle event fires, `ProtocolHeartBeatHandler` calls
`Heartbeater.triggerHeartBeat(connection)`.

`DefaultHeartbeater` sends a heartbeat `RequestMessage` over the connection. On
success it resets `connection.heartbeatFailCnt`; on failure it increments the counter.
When `failCnt >= heartbeatMaxFailCount` (default `3`), the connection is closed — which
in turn triggers `channelInactive → close(conn) → reconnector.onUnhealthy(addr)`.

Heartbeat can be suppressed per-`Connection` or per-`SocketAddress` via
`disableHeartBeat` / `enableHeartBeat`.

## Reconnection

`ClientConnectionManager.reconnector()` returns a `Reconnector` that drives
per-endpoint reconnect tasks with backoff + jitter. The Reconnector is documented in
detail in [reconnect.md](./reconnect.md). The connection between the two:

- `AbstractConnectionManager.close(conn)` calls `reconnector.onUnhealthy(addr)` after
  removing the connection from its pool (only if the reconnector is started).
- `AbstractConnectionManager.disconnect(addr)` calls `reconnector.cancel(addr)`
  before removing the pool.
- `ServerConnectionManager.reconnector()` returns `null`; the abstract base
  null-checks before every call.

## Configuration

```java
RemotingClientConfig clientConfig = new RemotingClientConfig();
clientConfig.setConnectionFactoryConfig(connectionFactoryConfig);   // optional
clientConfig.setConnectionManagerConfig(connectionManagerConfig);   // optional
clientConfig.setReconnectConfig(reconnectConfig);                   // optional
RemotingClient client = new RemotingClient(clientConfig);
```

`ConnectionFactoryConfig` knobs (all optional, sensible defaults):

| Field                  | Default | Effect                                              |
|------------------------|---------|-----------------------------------------------------|
| `idleSwitch`           | `true`  | Install `IdleStateHandler` and run heartbeats.      |
| `idleReader/Writer/AllTimeout` | 15000 ms | Idle thresholds.                          |
| `connectTimeout`       | 1000 ms | Netty `CONNECT_TIMEOUT_MILLIS` + the await ceiling. |
| `executor` / `timer`   | `null`  | Optional shared `ExecutorService` / `Timer` for `Connection` callbacks. When `null`, `DefaultConnectionFactory` owns its own and closes them on `shutdown`. |

`ConnectionManagerConfig`:

| Field                          | Default | Effect                            |
|--------------------------------|---------|-----------------------------------|
| `connectionNumPerEndpoint`     | `1`     | Pool size per address.            |

`ReconnectConfig` — see [reconnect.md](./reconnect.md).

## Server-side specialisation

`ServerConnectionManager` reuses the abstract base for `add`, `close`, `disconnect`,
`get`, and `check`, but:

- `connect(addr)` throws `UnsupportedOperationException` — the server does not dial.
- `reconnector()` and `heartbeater()` return `null` — the server does not retry or
  ping clients.

The same `ConnectionEventHandler` runs on the server pipeline, so listeners receive
`CONNECT` / `CLOSE` for every accepted channel.
