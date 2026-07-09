# 配置参考

> [📖 索引](README.zh-CN.md) · 上一篇：[← RPC 使用](rpc-usage.zh-CN.md) · 下一篇：[ConnectionManager →](connection-manager.zh-CN.md) · [🇬🇧 English](configuration-reference.md)

x-remoting 的所有可调项都在五个 config 类里。它们全部 **immutable**，用 builder 构造，`build()` 时校验。

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

控制出站连接的创建。

| 字段                  | 默认值  | 作用                                                      |
|----------------------|---------|----------------------------------------------------------|
| `idleSwitch`         | `true`  | 装 `IdleStateHandler`，让心跳能跑                          |
| `idleReaderTimeout` (ms) | 15000 | 读 idle 阈值                                             |
| `idleWriterTimeout` (ms) | 15000 | 写 idle 阈值                                             |
| `idleAllTimeout` (ms)    | 15000 | all-idle 阈值（读**和**写）                              |
| `connectTimeout` (ms)    | 1000  | Netty `CONNECT_TIMEOUT_MILLIS` + await 上限              |
| `useFlushConsolidation`  | `false` | 启用 Netty 的 `FlushConsolidationHandler`（提吞吐）     |
| `executor`           | `null`  | 共享的 `ExecutorService`，给 `Connection` 回调用（生命周期归调用方） |
| `timer`              | `null`  | 共享的 `Timer`，给请求超时用（生命周期归调用方）          |

```java
ConnectionFactoryConfig factory = ConnectionFactoryConfig.builder()
        .connectTimeout(2000)
        .idleAllTimeout(30_000)
        .useFlushConsolidation(true)
        .build();
```

**校验**：`connectTimeout > 0`；`idleSwitch=true` 时三个 idle timeout 至少一个 `> 0`。

## `ConnectionManagerConfig`

控制池大小。

| 字段                          | 默认值 | 作用                |
|------------------------------|--------|--------------------|
| `connectionNumPerEndpoint`   | 1      | 单地址连接池大小    |

```java
ConnectionManagerConfig pool = ConnectionManagerConfig.builder()
        .connectionNumPerEndpoint(4)
        .build();
```

**校验**：`connectionNumPerEndpoint > 0`。

## `ReconnectConfig`

控制重连状态机。完整模型见[重连](reconnect.zh-CN.md)。

| 字段                       | 默认值                                  | 作用                                                  |
|---------------------------|-----------------------------------------|------------------------------------------------------|
| `backoffPolicy`           | `ExponentialBackoffWithJitter.defaults()` | 两次尝试间的延迟（1s → 2s → 4s → 30s，±50% jitter）  |
| `maxAttempts`             | `-1`（无限）                            | 失败次数上限                                          |
| `maxTotalDurationNanos`   | `-1`（无限）                            | 累计重连时长上限                                      |
| `workerThreads`           | 4                                       | 跑阻塞 `connect()` 的线程池                          |

```java
ReconnectConfig reconnect = ReconnectConfig.builder()
        .backoffPolicy(new ExponentialBackoffWithJitter(500, 10_000, TimeUnit.MILLISECONDS, 2.0, 0.3))
        .maxAttempts(50)
        .maxTotalDurationNanos(TimeUnit.MINUTES.toNanos(10))
        .workerThreads(8)
        .build();
```

**校验**：`backoffPolicy != null`、`workerThreads > 0`。

### 内置 `BackoffPolicy`

| 类                              | 行为                                                       |
|---------------------------------|-----------------------------------------------------------|
| `ExponentialBackoffWithJitter`  | **默认**。指数退避，initial / max / multiplier / jitter 都可调 |
| `FixedIntervalBackoff`          | 固定间隔                                                  |
| `NoReconnectPolicy`             | 返回 `-1` —— 永不重连（等于关掉重连）                    |

自己实现 `BackoffPolicy.nextDelayNanos(int attempts)`；返回负值表示"放弃"。例子见[扩展](extending.zh-CN.md)。

## `RemotingClientConfig`

把上面三个 config 组装到 `RemotingClient` 的容器。仍然是可变的（Lombok `@Data`），因为它就是构造时用一次的过渡 wrapper。

| 字段                       | 默认值 | 作用                                          |
|---------------------------|--------|----------------------------------------------|
| `connectionFactoryConfig` | `null` → `ConnectionFactoryConfig.defaults()` | 传给 `ClientConnectionManager` |
| `connectionManagerConfig` | `null` → `ConnectionManagerConfig.defaults()` | 传给 `ClientConnectionManager` |
| `reconnectConfig`         | `null` → `ReconnectConfig.defaults()`         | 传给 `ClientConnectionManager` |

## `RemotingServerConfig`（继承 `ServerConfig`）

| 字段                  | 默认值  | 作用                                              |
|----------------------|---------|--------------------------------------------------|
| `hostName`           | `null`（绑所有 interface） | 绑定 host                          |
| `port`               | 0（随机端口） | 绑定 port                                      |
| `manageConnection`   | `false` | 保留入站连接，让服务端能反向调用                  |
| `idleSwitch`         | `true`  | 装 `IdleStateHandler`                           |
| `idleReaderTimeout` (ms) | 0 | 读 idle 阈值（0 = 关）                              |
| `idleWriterTimeout` (ms) | 0 | 写 idle 阈值（0 = 关）                              |
| `idleAllTimeout` (ms)    | 90000 | all-idle 阈值                                   |
| `serializationType`  | `Hession` | 响应消息的默认序列化器                          |
| `executor`           | `null` → cached 池 | handler 执行池                       |
| `timer`              | `null` → 内置 `HashedWheelTimer` | 服务端 timeout 调度       |

```java
RemotingServerConfig server = new RemotingServerConfig();
server.setPort(8989);
server.setManageConnection(true);
server.setIdleAllTimeout(60_000);
```

`ServerConfig` 暂时还是 `@Data`（可变 + setter）—— 是 builder 改造之前的设计，可能在后续版本迁移。

## `CallOptions`（per-call）

不可变，通过 `CallOptions.builder()...build()` 或 `CallOptions.defaults()` 构造。每次调用都要传：

| 字段                | 默认值                  | 作用                                  |
|--------------------|------------------------|--------------------------------------|
| `timeoutMills`     | 3000                   | per-call 超时                         |
| `serializationType` | `Hession`             | per-call 序列化器覆盖                |
| `headers`          | 空 `DefaultMessageHeaders` | 自定义消息 header                |

---

> [📖 索引](README.zh-CN.md) · 上一篇：[← RPC 使用](rpc-usage.zh-CN.md) · 下一篇：[ConnectionManager →](connection-manager.zh-CN.md)
