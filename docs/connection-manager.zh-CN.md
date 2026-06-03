# ConnectionManager

> English: [connection-manager.md](./connection-manager.md)

`ConnectionManager` 是 x-remoting 中持有和复用 TCP 连接的中心抽象。客户端侧按远端地址维护一组常驻连接池；服务端侧索引由对端接入的连接。围绕连接的建立、健康监测、故障响应、生命周期事件，全部收敛到这一个接口背后。

## 类型关系

```
                                 ConnectionManager  (接口，继承 LifeCycle)
                                          ▲
                                          │
                          ┌───────────────┴───────────────┐
                          │                               │
              AbstractConnectionManager           (共用基类)
                          │
                ┌─────────┴────────────┐
                │                      │
   ClientConnectionManager       ServerConnectionManager
   (主动发起方，                  (登记由对端接入的连接，
    持 Reconnector + Heartbeater) 不发起 connect)
```

协作组件：

| 类型                          | 职责                                                      |
|------------------------------|-----------------------------------------------------------|
| `Connection`                 | 对 Netty `Channel` 的封装，含 protocol、executor、timer 与未完成请求的 `InvokeFuture` map。 |
| `Connections`                | 单个地址的连接池，带 closed 标志和安全 `add()`。 |
| `ConnectionFactory`          | 负责把一个地址变成一个 `Connection`。 |
| `DefaultConnectionFactory`   | 基于 Netty `Bootstrap` 的默认实现。 |
| `ConnectionFactoryConfig`    | 空闲超时、连接超时、可选共享的 `Executor` / `Timer`。 |
| `ConnectionManagerConfig`    | 连接池大小（`connectionNumPerEndpoint`）。 |
| `ConnectionSelectStrategy`   | 从池里选一条连接（默认轮询）。 |
| `ConnectionEventProcessor`   | 异步把 `CONNECT`/`CLOSE` 事件分发给监听者。 |
| `ConnectionEventListener`    | 用户回调，监听 `CONNECT`/`CLOSE`。 |
| `ConnectionEventHandler`     | Netty handler，把 `channelInactive`/`exceptionCaught` 桥接回 manager 并触发事件。 |
| `Heartbeater`                | 由 Netty `IdleStateEvent` 触发，发送心跳，计数失败，达到阈值则关闭。 |
| `Reconnector`                | 每个 endpoint 一个的重连状态机，详见 [reconnect.md](./reconnect.md)。 |

## ConnectionManager 接口

```java
public interface ConnectionManager extends LifeCycle {
    Connection connect(InetSocketAddress addr) throws RemotingException;   // 建池，返回一条
    void       disconnect(InetSocketAddress addr);                          // 拆池
    Connection get(InetSocketAddress addr) throws RemotingException;        // get-or-connect
    void       check(Connection conn) throws RemotingException;         // 健康检查
    void       close(Connection conn);                                  // 单条连接下线
    void       add(Connection conn);                                    // 登记一条连接（服务端）
    Reconnector              reconnector();                             // 服务端为 null
    ConnectionEventProcessor connectionEventProcessor();
    Heartbeater              heartbeater();                             // 服务端为 null
}
```

## 池结构

```
ClientConnectionManager
   └── connectionsMap : ConcurrentHashMap<InetSocketAddress, Connections>
         ├── 10.0.0.1:8080 → Connections [Conn1, Conn2, Conn3]   (size = N)
         ├── 10.0.0.2:8080 → Connections [Conn4, Conn5, Conn6]
         └── …
```

- `connectionsMap` 是 `ConcurrentHashMap`，key 是远端 `SocketAddress`。
- 每个 `Connections` 内部用 `CopyOnWriteArrayList<Connection>` 存连接，外加一个 `volatile boolean closed` 标志。一旦 `close()`，后续 `add()` 不会泄漏 —— 进来的连接会被立即关闭。
- 单地址池大小由 `ConnectionManagerConfig.connectionNumPerEndpoint` 控制（默认 `1`）。
- 选连接走 `ConnectionSelectStrategy`，默认是 `RoundRobinConnectionSelectStrategy`（`AtomicInteger` 计数轮询）。

## Connection 内部

`Connection` 包一个 Netty `Channel`，并持有：

- `Protocol` —— 这条链路上的编解码与消息工厂。
- `Executor` —— 用户回调（如 `InvokeCallBack`）执行的线程池；默认是 `DefaultConnectionFactory` 内部的共享池。
- `Timer` —— `HashedWheelTimer`，用于请求超时；同样可共享。
- `invokeMap : ConcurrentHashMap<Integer, InvokeFuture<?>>` —— 按 request id 索引的未完成 RPC。
- `closed : AtomicBoolean` —— `close()` 幂等。关闭时所有未完成的 `InvokeFuture` 都会以 `ConnectionClosed` 状态完成。

`Connection` 一构造完，就在 pipeline 上立刻 fire `ConnectionEvent.CONNECT`。

## 生命周期

```
startup()
  └── AbstractConnectionManager.startup()
        ├── super.startup()                        (started 置 true)
        └── connectionEventProcessor.startup()     (启动事件分发线程)
  └── ClientConnectionManager.startup() 额外：
        └── reconnector.startup()                  (启动 HashedWheelTimer + worker 池)

shutdown()
  └── ClientConnectionManager.shutdown()
        ├── super.shutdown()                       (依次 disconnect 所有地址)
        ├── connectionFactory.close()              (关闭 Netty EventLoopGroup)
        └── reconnector.shutdown()                 (取消所有 timer，等 worker 退出)
```

`AbstractLifeCycle.shutdown()` 用 CAS 保护 started flag，重复 `shutdown()` 会抛 `IllegalStateException`。

## 端到端流程

### `get(addr)` —— 正常路径

```
get(addr)
  ├── connectionsMap.get(addr)
  │     ├── 存在  → connections.get() → strategy.select(snapshot) → 返回 Connection
  │     └── 不存在 → 走到 connect
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

`DefaultConnectionFactory.create(addr)` 内部 `bootstrap.connect(addr)`，再以 `await(connectTimeout + 100ms)` 等待 future。中断会被尊重（取消在途 connect，并抛 `RemotingException`）。建好的 `Connection` 会被设置为 channel 的 `CONNECTION` 属性，并通过 pipeline fire `ConnectionEvent.CONNECT`。

### `close(connection)` —— 被动下线

由 `ConnectionEventHandler.channelInactive` 触发（链路断、对端 reset、idle 关、心跳关、异常 …），以及 `check()` 健康检查失败时也会调用。

```
NIO 线程 → channelInactive
   └── if (connectionManager.isStarted()) connectionManager.close(conn)
   └── userEventTriggered(ConnectionEvent.CLOSE)

ConnectionManager.close(conn):
   ├── connections = connectionsMap.get(addr)
   ├── if connections == null  → conn.close(); return
   ├── connections.invalidate(conn)  → conn.close() + COW remove
   │     ├── true  → reconnector.onUnhealthy(addr)   (见 reconnect.md)
   │     └── false → 无动作
   └── if connections.isEmpty()  → connectionsMap.remove(addr, connections)
```

`remove(addr, connections)` 这个两参数版本只会在 map 中的值 **仍是同一个对象** 时才删除，避免删掉被并发 `add()` 重建的池。

### `disconnect(addr)` —— 主动下线

用户显式说"这个 endpoint 不要了"：

```
ConnectionManager.disconnect(addr):
   ├── reconnector.cancel(addr)                    (取消任何挂起的重连)
   ├── connections = connectionsMap.remove(addr)   (原子摘除)
   └── if connections != null: connections.close() (置 closed，关闭所有连接)
```

如果有并发的 `add()` 在 `remove()` 之后才落地，它会 compute 出一个 **新的** `Connections` 放回 map；老的会被干净关掉。反过来，如果 `add()` 在 `close()` 把老 `Connections` 标 closed 之后才把连接塞进去，`Connections.add()` 会直接把连接关掉，不会泄漏。

### `add(connection)` —— 服务端登记

```
ConnectionManager.add(conn):
   └── connectionsMap.compute(addr, (k, existing) -> {
         Connections cs = (existing != null) ? existing : new Connections(strategy);
         cs.add(conn);
         return cs;
       });
```

在 `connectionsMap` 上原子 create-or-append，配合 `Connections` 自己 closed-aware 的 `add()` 保证安全。

## 并发模型

重连机制重写时把 `AbstractConnectionManager` 上的全局 `synchronized` 全部去掉了：

| 操作              | 持有的锁                                          |
|------------------|---------------------------------------------------|
| `get(addr)`      | 热路径无锁。可能落到 `connect`。 |
| `connect(addr)`  | 仅 `synchronized (connections)` 包住填充循环，按地址互斥。 |
| `close(conn)`    | manager 上无锁。`Connections.invalidate` 基于 COW。 |
| `disconnect(addr)` | manager 上无锁。`Connections.close` 只翻一个 volatile flag。 |
| `add(conn)`      | `compute` 期间持 `ConcurrentHashMap` 的 bucket 锁，时间极短。 |

NIO 线程 `channelInactive → close(conn)` 不会被另一个地址上的慢 `connect()` 拖住。

## ConnectionEvent 分发

`ConnectionEvent` 枚举有两个值：`CONNECT`（在 `Connection` 构造器内 fire）、`CLOSE`（在 `ConnectionEventHandler.channelInactive` 里 fire）。它们由 `ConnectionEventHandler.userEventTriggered` 转给 `ConnectionEventProcessor.handleEvent(event, connection)`。

`DefaultConnectionEventProcessor` 把每个事件投到一个无界 `LinkedBlockingQueue`，由一个后台线程（`RemotingClient-Connection-Event`）取出后再依次回调每个注册过的 `ConnectionEventListener`。listener 抛异常会被 catch 并记日志，不影响其它 listener。事件投递是异步的，且全局有序（只有一个分发线程）。

## 心跳

心跳由 Netty 的 `IdleStateHandler` 驱动（通过 `ConnectionFactoryConfig.idleSwitch / idleReader / idleWriter / idleAll` 注入，默认 15 秒）。一旦 idle 事件触发，`ProtocolHeartBeatHandler` 调用 `Heartbeater.triggerHeartBeat(connection)`。

`DefaultHeartbeater` 在这条连接上发一个心跳 `RequestMessage`：成功就把 `connection.heartbeatFailCnt` 清零；失败就 +1。当 `failCnt >= heartbeatMaxFailCount`（默认 `3`）时关闭连接 —— 进而触发 `channelInactive → close(conn) → reconnector.onUnhealthy(addr)`。

可以按 `Connection` 或按 `SocketAddress` 维度通过 `disableHeartBeat`/`enableHeartBeat` 暂停心跳。

## 重连

`ClientConnectionManager.reconnector()` 返回的 `Reconnector` 以每个 endpoint 一个状态机的方式驱动重连，带退避 + 抖动。详细机制见 [reconnect.md](./reconnect.md)。它和 ConnectionManager 之间的衔接：

- `AbstractConnectionManager.close(conn)` 在把连接从池里移除后调 `reconnector.onUnhealthy(addr)`（前提是 reconnector 已启动）。
- `AbstractConnectionManager.disconnect(addr)` 在拆池前调 `reconnector.cancel(addr)`。
- `ServerConnectionManager.reconnector()` 返回 `null`，抽象基类在每个调用前都做了 null check。

## 配置

```java
RemotingClientConfig clientConfig = new RemotingClientConfig();
clientConfig.setConnectionFactoryConfig(connectionFactoryConfig);   // 可选
clientConfig.setConnectionManagerConfig(connectionManagerConfig);   // 可选
clientConfig.setReconnectConfig(reconnectConfig);                   // 可选
RemotingClient client = new RemotingClient(clientConfig);
```

`ConnectionFactoryConfig` 字段（全部可选，有合理默认值）：

| 字段                  | 默认值   | 作用                                            |
|----------------------|---------|-------------------------------------------------|
| `idleSwitch`         | `true`  | 注入 `IdleStateHandler` 并跑心跳。              |
| `idleReader/Writer/AllTimeout` | 15000ms | idle 阈值。                            |
| `connectTimeout`     | 1000ms  | Netty `CONNECT_TIMEOUT_MILLIS` 与 await 上限。  |
| `executor` / `timer` | `null`  | 可选的共享 `ExecutorService` / `Timer`。为 `null` 时 `DefaultConnectionFactory` 自己持有并在 `shutdown` 关闭。 |

`ConnectionManagerConfig`：

| 字段                          | 默认 | 作用                              |
|-------------------------------|------|-----------------------------------|
| `connectionNumPerEndpoint`    | `1`  | 单地址连接池大小。                |

`ReconnectConfig` —— 见 [reconnect.md](./reconnect.md)。

## 服务端的特化

`ServerConnectionManager` 复用抽象基类的 `add` / `close` / `disconnect` / `get` / `check`，但：

- `connect(addr)` 直接抛 `UnsupportedOperationException` —— 服务端不发起拨号。
- `reconnector()` 和 `heartbeater()` 都返回 `null` —— 服务端不重连、不主动 ping 客户端。

服务端 pipeline 上也跑同一个 `ConnectionEventHandler`，所以 listener 会收到每条被接入 channel 的 `CONNECT` / `CLOSE` 事件。
