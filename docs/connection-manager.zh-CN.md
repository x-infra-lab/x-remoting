# ConnectionManager

> [📖 索引](README.zh-CN.md) · 上一篇：[← 配置参考](configuration-reference.zh-CN.md) · 下一篇：[重连 →](reconnect.zh-CN.md) · [🇬🇧 English](connection-manager.md)

`ConnectionManager` 是持有和复用 TCP 连接的中心抽象。客户端按远端地址维护常驻连接池；服务端索引由对端接入的连接。

## 池结构

```
ClientConnectionManager
   └── connectionsMap : ConcurrentHashMap<InetSocketAddress, Connections>
         ├── 10.0.0.1:8080 → Connections [Conn1, Conn2, Conn3]   (size = N)
         ├── 10.0.0.2:8080 → Connections [Conn4, Conn5, Conn6]
         └── …
```

- `connectionsMap` 是 `ConcurrentHashMap`；单地址池大小由 `ConnectionManagerConfig.connectionNumPerEndpoint` 控制（默认 1）。
- 每个 `Connections` 内部用 `CopyOnWriteArrayList<Connection>` 存连接 + 一个 `volatile boolean closed` 标志。`close()` 之后再 `add()` 不会泄漏 —— 进来的连接会被立即关掉。
- `ConnectionSelectStrategy` 决定从活连接里选哪一条；默认是 `RoundRobinConnectionSelectStrategy`。

## Connection 内部

`Connection` 包一个 Netty `Channel`，外加：

- `Protocol` —— 这条链路的编解码 / 消息工厂
- `Executor` —— 用户回调跑的线程池（默认是 `DefaultConnectionFactory` 内部的池）
- `Timer` —— `HashedWheelTimer`，给请求超时用
- `invokeMap : ConcurrentHashMap<Integer, InvokeFuture<?>>` —— 按 request id 索引的未完成 RPC
- `closed : AtomicBoolean` —— 幂等 `close()`；未完成的 `InvokeFuture` 全部以 `ConnectionClosed` 状态完成

`Connection` 构造完会在 pipeline 上 fire `ConnectionEvent.CONNECT`。

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

`DefaultConnectionFactory.create(addr)` 内部 `bootstrap.connect(addr)`，再以 `await(connectTimeout + 100ms)` 等待 future。**中断会被尊重** —— 取消在途 connect 并抛 `RemotingException`。

### `close(connection)` —— 被动下线

由 `ConnectionEventHandler.channelInactive` 触发（链路断、对端 reset、idle 关、心跳关、异常），以及 `check()` 健康检查失败时也会调用。

```
ConnectionManager.close(conn):
   ├── connections = connectionsMap.get(addr)
   ├── if connections == null  → conn.close(); return
   ├── connections.invalidate(conn)  → conn.close() + COW remove
   │     ├── true  → reconnector.onUnhealthy(addr)
   │     └── false → 无动作
   └── if connections.isEmpty()  → connectionsMap.remove(addr, connections)
```

两参数版 `remove(addr, connections)` 只在 map 中的值仍是同一个对象时才删除 —— 防止删掉被并发 `add()` 重建的池。

### `disconnect(addr)` —— 主动下线

```
ConnectionManager.disconnect(addr):
   ├── reconnector.cancel(addr)                    (取消挂起的重连)
   ├── connections = connectionsMap.remove(addr)   (原子摘除)
   └── if connections != null: connections.close() (置 closed，关掉所有连接)
```

## 并发模型

热路径上没有全局 `synchronized`：

| 操作              | 持有的锁                                              |
|------------------|------------------------------------------------------|
| `get(addr)`      | 热路径无锁。可能走到 `connect`                       |
| `connect(addr)`  | 仅 `synchronized (connections)` 包住填充循环；按地址互斥 |
| `close(conn)`    | manager 上无锁。`Connections.invalidate` 走 COW       |
| `disconnect(addr)` | manager 上无锁。`Connections.close` 翻 volatile flag |
| `add(conn)`      | `compute` 期间持 `ConcurrentHashMap` bucket 锁，时间极短 |

NIO 线程 `channelInactive → close(conn)` 不会被另一个地址上的慢 `connect()` 拖住。

## 连接事件

`ConnectionEvent` 有两个值：`CONNECT`（在 `Connection` 构造器内 fire）、`CLOSE`（在 `channelInactive` 里 fire）。它们由 `ConnectionEventProcessor.handleEvent(event, connection)` 异步分发给每个注册过的 `ConnectionEventListener`。

```java
client.getConnectionManager().connectionEventProcessor()
      .addConnectionEventListener(new ConnectionEventListener() {
          @Override public void onEvent(ConnectionEvent evt, Connection conn) {
              log.info("{} {}", evt, conn.remoteAddress());
          }
      });
```

### 分发线程

默认所有事件都派发到一个叫 `RemotingClient-Connection-Event` 的单线程上。慢 listener 会拖累后续事件。两个 escape hatch：

- **per-listener executor**：listener 想做 I/O 等慢工作的话覆盖 `executor()`：
  ```java
  new ConnectionEventListener() {
      @Override public void onEvent(...) { /* slow work */ }
      @Override public Executor executor() { return myPool; }
  }
  ```
- **自定义 dispatcher**：构造 `DefaultConnectionEventProcessor(myExecutor)` 直接替换默认的单线程 dispatcher。executor 的生命周期归调用方。

## 心跳

心跳由 Netty 的 `IdleStateHandler` 驱动（`ConnectionFactoryConfig.idleSwitch=true` 时安装，默认开）。idle 事件触发时，`ProtocolHeartBeatHandler` 调 `Heartbeater.triggerHeartBeat(connection)`。

`DefaultHeartbeater` 在连接上发心跳 `RequestMessage`：

- **成功** → `connection.heartbeatFailCnt` 清零
- **失败** → `heartbeatFailCnt` 原子自增
- **`heartbeatFailCnt >= heartbeatMaxFailCount`**（默认 3）→ 关闭连接 → 触发 `channelInactive → close(conn) → reconnector.onUnhealthy(addr)`

可以按 `Connection` 或按 `InetSocketAddress` 暂停心跳：

```java
client.getConnectionManager().heartbeater().disableHeartBeat(connection);
client.getConnectionManager().heartbeater().disableHeartBeat(address);
```

两个 blocklist 都用 `ConcurrentHashMap.newKeySet()` 存，任意线程都可以安全修改。

## 生命周期

```
startup()
  └── AbstractConnectionManager.startup()
        ├── super.startup()                        (started = true)
        └── connectionEventProcessor.startup()     (启动 dispatcher)
  └── ClientConnectionManager.startup() 额外：
        └── reconnector.startup()                  (启动 HashedWheelTimer + worker 池)

shutdown()
  └── ClientConnectionManager.shutdown()
        ├── super.shutdown()                       (依次 disconnect 所有地址)
        ├── connectionFactory.close()              (关闭 Netty EventLoopGroup)
        └── reconnector.shutdown()                 (取消 timer，等 worker 退出)
```

`AbstractLifeCycle.shutdown()` 用 CAS 保护 started flag，重复 `shutdown()` 会抛 `IllegalStateException`。

## 服务端的特化

`ServerConnectionManager` 复用基类的 `add` / `close` / `disconnect` / `get` / `check`，但：

- `connect(addr)` 直接抛 `UnsupportedOperationException` —— 服务端不发起拨号
- `reconnector()` 和 `heartbeater()` 都返回 `null` —— 服务端不重连、不主动 ping 客户端。基类在每个调用前都做了 null check

服务端 pipeline 上跑同一个 `ConnectionEventHandler`，所以 listener 会收到每条被接入 channel 的 `CONNECT` / `CLOSE` 事件。

---

> [📖 索引](README.zh-CN.md) · 上一篇：[← 配置参考](configuration-reference.zh-CN.md) · 下一篇：[重连 →](reconnect.zh-CN.md)
