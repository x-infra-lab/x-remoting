# 扩展

> [📖 索引](README.zh-CN.md) · 上一篇：[← 重连](reconnect.zh-CN.md) · 下一篇：[FAQ →](faq.zh-CN.md) · [🇬🇧 English](extending.md)

本页覆盖最常用的几个扩展点。

## 自定义 `BackoffPolicy`

`BackoffPolicy` 决定两次重连尝试间等多久。

```java
public interface BackoffPolicy {
    /** 返回延迟纳秒数；返回负值表示"放弃"。 */
    long nextDelayNanos(int attempts);
}
```

例子 —— decorrelated jitter（AWS 推荐模式）：

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

## 自定义 `ReconnectListener`

[重连](reconnect.zh-CN.md)里讲过了。常见模式：

```java
// Micrometer 打点
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

## 自定义 `ConnectionEventListener`

监听每一次 `CONNECT` / `CLOSE`。需要时通过 `executor()` 让慢工作不堵 dispatcher：

```java
client.getConnectionManager().connectionEventProcessor()
      .addConnectionEventListener(new ConnectionEventListener() {
          @Override public void onEvent(ConnectionEvent evt, Connection conn) {
              auditLogService.record(evt, conn.remoteAddress());   // 慢 I/O
          }
          @Override public Executor executor() {
              return auditExecutor;        // 跑在我自己的池上
          }
      });
```

`executor()` 返回 null（默认）时，listener 同步跑在 dispatcher 上，会阻塞后续事件。

## 自定义 `ConnectionEventProcessor`

单线程 dispatcher 都嫌慢的话，自己塞个 executor：

```java
ExecutorService dispatcher = Executors.newFixedThreadPool(4,
        new NamedThreadFactory("my-event-dispatch"));
DefaultConnectionEventProcessor processor = new DefaultConnectionEventProcessor(dispatcher);
```

`dispatcher.shutdown()` 归调用方。（要把它装到 `ClientConnectionManager` 里目前得直接构造 manager —— 公开的 RPC 客户端还没暴露这个开关。）

## 自定义 `ConnectionSelectStrategy`

默认是用 `AtomicInteger` 计数的轮询。需要加权 / 粘性路由就塞自己的：

```java
public class StickyByThreadStrategy implements ConnectionSelectStrategy {
    @Override public Connection select(List<Connection> connections) {
        if (connections.isEmpty()) return null;
        int idx = (int) (Thread.currentThread().getId() % connections.size());
        return connections.get(idx);
    }
}
```

目前策略是 `AbstractConnectionManager` 的 `protected` 字段（`connectionSelectStrategy`）—— 子类覆盖。

## 自定义 `Heartbeater`

实现 `Heartbeater` 来改变心跳怎么发，或者根据应用状态门控。框架在每个 `IdleStateEvent` 上调 `triggerHeartBeat(Connection)`。默认实现负责"计数失败 + 超阈值关链"，自己替换的话记得复制这部分逻辑，否则死连接会泄漏。

## 自定义 `Protocol`（进阶）

`Protocol` 是线协议门面：暴露一个 `MessageCodec`、一个 `MessageFactory`、一个 `MessageHandler` 和唯一的 `ProtocolId`。要实现一个意味着自己负责：

1. 帧格式（长度前缀、magic、header 布局）—— 在 `MessageCodec` 里编解码
2. 消息类型（request、response、heartbeat）—— `MessageFactory.createXxx(...)`
3. 按类型派发 —— `MessageHandler` 和若干 `MessageTypeHandler`

`core` 里的 `RemotingProtocol` 是参考实现。如果你的需求就是 RPC，更建议扩展它（注册自己的 `RequestHandler`），而不是从头另写一个协议。

> 见[设计债](design-debt.zh-CN.md) —— `Protocol` 扩展点目前**更像理想而非现实**。周围的 `Message` / `MessageType` 体系都是为 `RemotingProtocol` 量身定做的。

## 多客户端共享 Netty `EventLoopGroup`

`DefaultConnectionFactory` 自己创建 `EventLoopGroup`。同一 JVM 里跑多个 client 想共享 Netty 线程，有两条路：

- 通过 `ConnectionFactoryConfig` 传共享的 `Executor` 和 `Timer` —— 至少回调和 timeout 是共享的（worker group 仍然各自一份）
- 直接用框架模块（`api`），自己写一个用共享 `EventLoopGroup` 的 `ConnectionFactory`，构造 `ClientConnectionManager`

完整支持还在 roadmap，不是 first-class 配置。

---

> [📖 索引](README.zh-CN.md) · 上一篇：[← 重连](reconnect.zh-CN.md) · 下一篇：[FAQ →](faq.zh-CN.md)
