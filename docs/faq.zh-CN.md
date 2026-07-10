# FAQ

> [📖 索引](README.zh-CN.md) · 上一篇：[← 扩展](extending.zh-CN.md) · [🇬🇧 English](faq.md)

## 公开 API 为什么用 `InetSocketAddress` 而不是 `SocketAddress`？

x-remoting 只支持 TCP。API 里用 `InetSocketAddress` 让用户少一层 cast，也明确"只支持 IP-based transport"。框架内部的 `Connection.remoteAddress()` 还是返回 `SocketAddress`（Netty 契约），在 `AbstractConnectionManager` 边界做 cast。

## 心跳到底什么时候发？

由 Netty 的 `IdleStateHandler` 驱动，`ConnectionFactoryConfig.idleSwitch=true`（默认）时安装。读 / 写 / 全空闲任一达到阈值（默认 15s）就触发。事件触发时 `DefaultHeartbeater` 发心跳请求，失败 → 失败计数 +1；连续失败 `heartbeatMaxFailCount` 次（默认 3）→ 关闭连接，重连流水线接手。

想要更长 idle 期不断连，要么调高阈值、要么 `idleSwitch=false` 完全关掉 idle check。

## `disconnect` / `cancel` / `disable` / `close` 有啥区别？

| 操作                                       | 对池的影响                | 对重连的影响                |
|-------------------------------------------|--------------------------|----------------------------|
| `ConnectionManager.close(connection)`     | 摘除一条连接              | 触发 `onUnhealthy(addr)`   |
| `ConnectionManager.disconnect(addr)`      | 拆掉该地址整个池          | 触发 `cancel(addr)`        |
| `Reconnector.cancel(addr)`                | （不影响池）              | 状态重置到 IDLE             |
| `Reconnector.disable(addr)`               | （不影响池）              | 暂停；`onUnhealthy` 被忽略 |
| `Reconnector.onUnhealthy(addr)`           | （不影响池）              | 排重连                     |

用户说"这个 endpoint 不要了"→ `disconnect`。单条死连接→ `close`（框架在 channelInactive 时会自动调）。要临时暂停重连（比如维护窗口）→ `disable`，恢复 → `enable`。

## 重连为什么要等 1 秒以上才发？

默认 `BackoffPolicy` 是 `ExponentialBackoffWithJitter`，起始 1s 带 ±50% jitter（第一次延迟约 500ms–1.5s）。**故意的** —— 立即重试会放大对端故障期的负载。需要更快恢复就换成 `FixedIntervalBackoff` 短间隔，或者自己写策略。需要**同步**重连直接调 `ConnectionManager.get(addr)`。

## 怎么接 OpenTelemetry / Micrometer？

重连事件：实现 `ReconnectListener`，在回调里打点（见[扩展](extending.zh-CN.md)）。

连接事件：实现 `ConnectionEventListener`，往 `connectionEventProcessor()` 里 add。

目前没有 first-class metrics 集成，在 roadmap 上。

## 能用 unix domain socket / TLS / HTTP transport 吗？

直接用不行。内置 `ConnectionFactory` 只走 TCP，公开 API 收 `InetSocketAddress`。你可以基于传输层写自定义 `ConnectionFactory`（也可能要自定义 `Protocol`）来支持其它 transport。

## 服务端怎么反向调用客户端？

`RemotingServerConfig.manageConnection=true`。开了之后服务端会把入站 channel 留在 `ServerConnectionManager` 里。客户端连上之后，服务端可以 `server.blockingCall(api, request, clientAddress, opts)` 把请求 push 回去。见 [RPC 使用](rpc-usage.zh-CN.md#服务端反向调用)。

## 为什么第二次调 `shutdown()` 抛 `IllegalStateException`？

`AbstractLifeCycle.shutdown()` 用 CAS 守卫 —— 生命周期状态正好转换一次。如果可能从多个 cleanup 路径调 shutdown，先用 `isStarted()` 守一下：

```java
if (client.isStarted()) {
    client.shutdown();
}
```

## 多客户端怎么共享 Netty 线程？

直接走 RPC API 不行。可以通过 `ConnectionFactoryConfig` 传共享的 `Executor` 和 `Timer`，至少这两样共享。完全共享 `EventLoopGroup` 得降到传输层自己写 `ConnectionFactory`。first-class 支持还在 roadmap。

## macOS / Windows 上能用吗？

可以，开发和生产都行。Linux 上加 `netty-transport-native-epoll:linux-x86_64`（或 `:linux-aarch64`）依赖能启用 Epoll，更高吞吐。没装就自动降到 NIO。

## 怎么换序列化器（Kryo / Protobuf）？

目前 `SerializationType` 是 enum，只发了 `Hession`。换序列化器是 roadmap 项 —— 注册表需要做 SPI 化。在那之前，你可以手动把 payload 序列化成 `byte[]` 字段，服务端再 unwrap。

## listener 抛异常会不会影响其它 listener？

不会。dispatcher 每次调 listener 都包了 `try { ... } catch (Throwable t) { log.warn(...); }`。异常被记录，下一个 listener 继续。`ReconnectListener` 同。

## x-remoting 创建的是 daemon 线程吗？

Netty `EventLoopGroup` 是 daemon（Netty 默认），但 `RemotingClient-Reconnect-Worker-*` 池、事件 dispatcher 线程、服务端 cached executor 都不是 daemon。**一定记得调 `shutdown()`**，JVM 才能正常退出。

---

> [📖 索引](README.zh-CN.md) · 上一篇：[← 扩展](extending.zh-CN.md)
