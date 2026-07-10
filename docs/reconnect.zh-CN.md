# 重连机制

> [📖 索引](README.zh-CN.md) · 上一篇：[← ConnectionManager](connection-manager.zh-CN.md) · 下一篇：[扩展 →](extending.zh-CN.md) · [🇬🇧 English](reconnect.md)

x-remoting 的重连器是一个**按 endpoint 一个一个的状态机**，带退避、抖动、上限和生命周期事件。运维人员通过 `ReconnectListener` 观察，框架自己处理剩下的事。

## 设计目标

- **endpoint 之间隔离** —— 一个慢或不可达的 endpoint 不能拖累其它 endpoint 的重连。
- **默认退避 + 抖动** —— 避免重试风暴和多客户端同步雷暴。
- **重试有上限** —— 运维可以设次数 / 总时长，超出后端点进入 *abandoned*，listener 收到通知，应用可以反应（比如从负载均衡里剔除）。
- **热路径上没有全局锁** —— reconnector 不能阻塞 Netty I/O 线程。
- **单一事实源** —— 每个 endpoint 一个状态，公开 API 可观测。

## 状态机

每个被 reconnector 追踪的 `InetSocketAddress` 持有一个 `EndpointReconnectTask`，状态取以下之一：

```
                  ┌──────── disable() ────────┐
                  ▼                            │
              DISABLED ◀─── disable() ─── (除 STOPPED 外任意)
                  │                            │
                enable()                       │
                  ▼                            │
     ┌─────►  IDLE  ◀─────── 成功 ───────────┤
     │         │                               │
     │   onUnhealthy()                         │
     │         ▼                               │
     │     SCHEDULED ── 计时器到点 ─►  CONNECTING
     │         │                               │
     │     cancel()              ┌─────────────┴───────────┐
     │         │                 │                         │
     │         ▼              成功                       失败
     │       IDLE                │                         │
     │                           │                  attempts++ ?
     │                           ▼                         │
     │                          IDLE              ◀──────┴──────► ABANDONED
     │                                                                │
     └──────────────────── enable() ──────────────────────────────────┘
```

| 状态        | 含义                                                                       |
|------------|--------------------------------------------------------------------------|
| `IDLE`     | 无重连活动。初始状态、成功后的状态                                          |
| `SCHEDULED`| 计时器在等下一次尝试                                                       |
| `CONNECTING`| 在 worker 线程上正在跑阻塞 connect                                         |
| `DISABLED` | 用户暂停了这个 endpoint；挂起的计时器已取消                                  |
| `ABANDONED`| 达到 max attempts / max duration；用户必须 `enable()` 才能重试              |
| `STOPPED`  | reconnector 已经 shutdown                                                  |

状态切换在 per-endpoint 锁内进行；listener 回调在锁**外**触发，用户代码不会跟 reconnector 调用死锁。

## 重连何时触发？

reconnector 本身是纯被动的 —— 不会主动 probe。在下面之一调 `onUnhealthy(address)` 时才会排尝试：

1. **`AbstractConnectionManager.close(connection)`** —— Netty `channelInactive` 触发（链路断、对端 reset、心跳关 …）。连接从池里被摘除后，地址被标 unhealthy，让池可以重新填满。
2. **应用代码**也可以直接调 `reconnector.onUnhealthy(address)`。

反过来，下面会调 `cancel(address)` 清理任何挂起的尝试：

- `ConnectionManager.disconnect(address)` —— 用户显式拆掉了这个 endpoint，不要再连了。

## API

```java
public interface Reconnector extends LifeCycle {
    void onUnhealthy(InetSocketAddress address);   // 幂等；非 IDLE 时忽略
    void cancel(InetSocketAddress address);        // 用户主动 disconnect；清理状态
    void disable(InetSocketAddress address);       // 暂停；直到 enable() 才恢复
    void enable(InetSocketAddress address);        // 从 DISABLED / ABANDONED 恢复
    ReconnectState stateOf(InetSocketAddress address);
    void addListener(ReconnectListener listener);
}
```

### 方法语义

- **`onUnhealthy`** 仅当 endpoint 处于 `IDLE` 时才排重连。`SCHEDULED` / `CONNECTING` 状态下调用是安全的 no-op。`DISABLED` / `ABANDONED` / `STOPPED` 状态下默默忽略 —— **故意的**：避免一连串 `channelInactive` 事件把用户主动关掉的 endpoint 又"激活"回来。
- **`cancel`** 即使 reconnector 已经 shutdown 也能调（永远不会抛 `IllegalStateException`）。clean-up 路径可以放心用。
- **`disable` / `enable`** 是黏性的。disable 取消任何在途的计时器；enable 重置 attempts，回到 `IDLE`（**不会**立刻重连 —— 下一次 `onUnhealthy` 才开始新循环）。

## 监听器

```java
public interface ReconnectListener {
    default void onScheduled(InetSocketAddress address, int attempts, long delayNanos) {}
    default void onSuccess  (InetSocketAddress address, int attempts) {}
    default void onFailure  (InetSocketAddress address, int attempts, Throwable cause) {}
    default void onAbandoned(InetSocketAddress address, int attempts, Throwable lastCause) {}
}
```

`reconnector.addListener(...)` 注册。listener 按注册顺序串行调用，在 per-endpoint 锁外执行；某个 listener 抛异常会被日志记录，不影响其它 listener。

典型用途：

- 打点（`reconnect.attempts` / `reconnect.abandoned`）
- 把 abandoned 的地址从服务发现缓存里剔除
- 关键 endpoint 触发 `onAbandoned` 时报警

## 配置

```java
ReconnectConfig cfg = ReconnectConfig.builder()
        .backoffPolicy(ExponentialBackoffWithJitter.defaults())
        .maxAttempts(20)                                  // -1 = 无限
        .maxTotalDurationNanos(TimeUnit.MINUTES.toNanos(10))  // -1 = 无限
        .workerThreads(4)
        .build();
```

`build()` 会校验，非法值抛 `IllegalArgumentException`（`backoffPolicy=null`、`workerThreads <= 0`）。

### 退避策略

| 类                                | 行为                                                          |
|----------------------------------|--------------------------------------------------------------|
| `ExponentialBackoffWithJitter`   | **默认**。`1s → 2s → 4s → … → 30s` 封顶，±50% jitter         |
| `FixedIntervalBackoff(interval)` | 固定间隔（兼容旧行为）                                       |
| `NoReconnectPolicy`              | 返回 `-1` —— 永不重试；等于关掉重连                          |

自定义实现 `BackoffPolicy.nextDelayNanos(int attempts)`；返回负值表示"放弃"，endpoint 进 `ABANDONED`。例子见[扩展](extending.zh-CN.md)。

## 用例

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
        log.error("把 {} 从轮询里剔除，失败 {} 次", addr, attempts, lastCause);
        serviceRegistry.evict(addr);
    }
});
```

## 内部实现

- **调度** 用 reconnector 自己持有的 Netty `HashedWheelTimer`（`RemotingClient-Reconnect-Timer`）。计时器到点只往 worker 池投个任务 —— 阻塞 `connect()` 绝不在 timer 线程里跑。
- **Worker 池** 是固定大小 `ExecutorService`（`RemotingClient-Reconnect-Worker-*`，size = `workerThreads`）。一个慢 endpoint 占一个 worker；其它 endpoint 只要池没打满都能继续进展。
- **per-endpoint 状态机** 每个 `EndpointReconnectTask` 一个 `Object` 锁。不同 endpoint 上的并发操作互不干扰；同一 endpoint 上的操作都很短（锁内不做阻塞 I/O —— `connect()` 在锁外执行，结果再回过头在锁内 reconcile）。
- **与 `cancel` / `disable` 的竞态**：`connect()` 在途时，如果用户 cancel / disable，reconnector 在 reconcile 结果时会观察到状态变化，不再排下次。如果 connect 已经成功了才发现 cancel，新连接就留在池里 —— 后续 `disconnect()` 会关掉它。

## Shutdown 语义

`Reconnector.shutdown()`：

1. 把每个已追踪的 task 标 `STOPPED`，取消其挂起的计时器
2. 触发 `ExecutorService.shutdown()`
3. 最多等 **5 秒**让在途的 `connect()` 跑完；超时则 `shutdownNow()` 中断
4. 停 `HashedWheelTimer`

宿主 `ClientConnectionManager` 调 `reconnector.shutdown()` 的时机是**先**断开所有地址、关闭底层 `ConnectionFactory` **之后**。

## 注意事项

- **`onUnhealthy` 不阻塞**。第一次尝试要等策略的初始延迟（默认 ~1s）。需要同步重连就直接调 `ConnectionManager.get()` / `connect()`。
- **listener 必须快**。跑在重连 worker 线程上；慢 listener 会拖延下次排期并占用 worker 槽位。
- **`ABANDONED` 后 `enable()` 不会自动**。**故意的** —— 一个被放弃的 endpoint 该不该恢复，应该是运维或服务发现层显式决定的事情。
- **`ServerConnectionManager.reconnector()` 返回 `null`**。`AbstractConnectionManager` 所有 reconnect 调用点都做了 null check。

---

> [📖 索引](README.zh-CN.md) · 上一篇：[← ConnectionManager](connection-manager.zh-CN.md) · 下一篇：[扩展 →](extending.zh-CN.md)
