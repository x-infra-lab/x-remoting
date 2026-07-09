# 设计债

> [📖 索引](README.zh-CN.md) · 上一篇：[← FAQ](faq.zh-CN.md) · 下一篇：[路线图 →](roadmap.zh-CN.md) · [🇬🇧 English](design-debt.md)

这是一份诚实、有立场的架构债和设计 smell 清单，记录截至 `35bb2ce` 时代码库里还存在的问题。目的是对后来的贡献者**有用**，而不是客气。按伤害位置分组、按严重度打标：

- 🔴 —— 承重型架构问题；"框架"宣言在这里崩塌
- 🟠 —— 有意义的设计 smell 或陷阱；1.0 之前应该清理
- 🟡 —— 打磨 / 卫生级别；顺手做就行

哪些做得好的，最后承认。

## TL;DR

> 主要的分层问题已解决：传输层现在是真正的协议无关框架，`Connection` 是精简的封装，
> `Protocol` 是真正可扩展的。剩余债务主要在 RPC 层内部（`MessageTypeHandler` 过度继承、
> 线程池命名等）。

---

## 架构 smell

### ~~🔴 传输层 vs RPC 层曾经是名义分层~~ ✅ 已解决

传输层现在是真正的协议无关框架。所有 RPC 特有概念（`Call`、`InvokeFuture`、
`InFlightRequests`、`MessageType`、`RequestMessage`、`ResponseMessage`、
`AbstractMessageHandler`、`Heartbeater`、`HeartbeatState`）在 `rpc.*` 包中。
传输层的 `Message` 是标记接口；`Protocol` 只暴露 codec + handler。
非 RPC 协议可以在传输层之上构建，无需继承 RPC 脚手架。

### ~~🔴 `Connection` 是 god object~~ ✅ 已解决

`Connection` 已精简为：Channel、Protocol、Executor、Timer、close hooks 和 `closed` flag。
RPC 特有状态（`InFlightRequests`、`HeartbeatState`）现在作为 Netty channel attribute 存在于
`rpc` 层，通过 close hooks 注册清理逻辑。

### ~~🔴 `Protocol` 是假扩展点~~ ✅ 已解决

`Protocol` 现在是最小接口（codec + message handler）。所有 RPC 特有类型
（`Message`、`RequestMessage`、`ResponseMessage`、`MessageType`、`MessageTypeHandler`、
`MessageFactory`）已移至 `rpc` 包。新增 `RpcProtocol` 子接口提供
`getMessageFactory()`。非 RPC 协议现在可以直接实现 `Protocol` 而无需碰任何 RPC 抽象。

### 🔴 `MessageType` / `MessageTypeHandler` 家族是过度继承的典型

链路：

```
MessageHandler (iface)
└─ AbstractMessageHandler
   └─ MessageTypeHandler (iface)
      └─ AbstractRequestMessageTypeHandler
         ├─ HeartbeatRequestMessageTypeHandler
         └─ RemotingRequestMessageTypeHandler
      └─ ResponseMessageTypeHandler
```

**6 个类型搭一个 3 值枚举派发**。等价于一个 `Map<MessageType, BiConsumer<Connection, Message>>` 加三个 lambda。

**修法方向**：折叠成单个 dispatcher 类；让用户注册 lambda / method reference 来加新类型。

---

## 公开 API 陷阱

### 🟠 `Connection.remoteAddress()` 返回 `SocketAddress`，其它地方都收 `InetSocketAddress`

为了 EmbeddedChannel 测试不挂（它的 `remoteAddress()` 返回 `EmbeddedSocketAddress`，不是 `InetSocketAddress`）做的实用主义妥协。每个内部调用方都要 cast。

**修法方向**：加一个 `inetRemoteAddress()` 一次性 cast，或者把那几个用了 EmbeddedChannel + 真 Connection 的测试要么 mock 掉 connection、要么换成真 loopback channel。

### 🟠 `RequestApi.of("path")` 什么都没加

就一个 `String` wrapper，没有 metadata、没有 non-blank 之外的 validation、没有 enum-like 约束。**构造一次一个对象分配**，**零 type safety**（任何字符串都是合法 `RequestApi`）。

**修法方向**：要么直接收 `String`，要么把 `RequestApi` 升级成带 metadata（版本、超时、sealed 注册表）的东西。

### 🟠 `RequestHandler<T, R>` 是一个混乱的半成品 async SPI

```java
R handle(T request);                                // sync
default void asyncHandle(T, ResponseObserver<R>);   // 默认包 sync
default Executor getExecutor();                     // executor 覆盖
```

三个方法，其中 `handle` 即使你只想做 async 也必须实现；想做 async 时**还得记得**覆盖 `asyncHandle`，不然你 sync 的 `handle` 会被默默跑在框架的 executor 上。

**修法方向**：就一个 `void handle(T, ResponseObserver<R>)`。提供 `BlockingRequestHandler<T, R>` 适配器把 sync 函数包起来。大部分用户会用 blocking adapter，SPI 本身保持单一形态。

### 🟠 `CallOptions` 是 `@Data` 可变的，per-call 用

其它所有 `*Config` 都迁到 immutable + builder 了，唯独 `CallOptions` 漏掉。它是**最热**的 config —— 每次调用都用，而且经常被共享传递；跨线程改字段就是真 race。

**修法方向**：同样的 builder 处理。

### 🟠 `IDGenerator` 是 JVM 级 `AtomicInteger`

- 一个 JVM 里每个 `RemotingClient`（以及每个做反向调用的 `RemotingServer`）共享同一个计数器
- `Integer` 自动装箱在热路径上每次都 box —— 小事但没必要
- 2³¹ 之后回绕成负数 —— `Connection.addInvokeFuture` 有 `putIfAbsent` 守护，但回绕后撞上未完成请求会抛 `IllegalStateException` 而不是给个合法 id

**修法方向**：per-`Connection` 或 per-`Client` 的 `AtomicLong`。Long 给你 ~3000 万年（按 10k QPS）。

---

## 代码 smell 集

### 🟠 `@AccessForTest` 满天飞

注解是自造的，模式是"我把这个 private/protected 了但测试需要"。代码库里 14 处。它是"这些类难以独立测试"的承认 —— 也就是设计错了，不是测试框架缺位。

**修法方向**：把那些违规者重构得能测（小类、少 collaborator、可注入依赖），或者干脆改 package-private，注解都不要。

### 🟠 线程名 `RemotingClient-Server-Default-Executor-*`

**服务端**里跑着一个叫 `RemotingClient-` 前缀的池。从 `DefaultConnectionFactory` 拷过来写 `AbstractServer` 时没改名。**生产 jstack 拉出来运维会怀疑人生**。

**修法**：rename，几分钟的事。`RemotingClient-Server-Timer` 同样。

### 🟠 `Resource<T>` 抽象过度工程

`DefaultConnectionFactory` 内部用自造的 `Resource<T>` 接口包 lazy-init executor / timer（`get` + `close`）。同样的东西用 JDK 的 `Supplier<T>` + `Closeable` 就能描述。自造抽象多了一层 indirection，没买到任何东西。

**修法方向**：删 `Resource`，用 `Supplier` + 显式 close。

### 🟠 `Validate` 是自造的 `Preconditions`

`Validate.notNull` / `Validate.isTrue` 等 —— 已经有 Apache Commons `Validate`（甚至同名）和 Guava `Preconditions`。要么选一个删掉本地的，要么文档解释为什么要自造一份。

### 🟡 `IDGenerator.nextRequestId()` 返回 `Integer` 而不是 `int`

每次调用都强制装箱。装箱值随后又被 unbox 到一个 `int` 字段。纯浪费。

---

## 运行时 / 运维债

### 🔴 默认 Hessian + 没有安全态度

`SerializationType.Hession`（笔误也没改）是默认序列化器。Hessian 历年多个反序列化 RCE CVE；上游 `com.caucho:hessian` 自 2022 年再无发版。

仓库里**没有 `SECURITY.md`**、没有威胁模型文档、没有反序列化类型白/黑名单。

**修法方向（短期）**：加 `SECURITY.md` 写明漏洞披露渠道；文档明确"默认序列化器不能用在不可信网络上"。**（长期）**：`SerializationType` 改 SPI；把 Kryo / Protobuf 作为 first-class 选项发出去。

### 🟠 写路径无背压

`Channel.isWritable()` 在 `ConnectionManager.check()` 里查一次。之后 `Call.writeAndFlush` 不管高水位继续写。bursty producer + 慢 consumer = 出站 buffer 无界增长 = OOM。

**修法方向**：写之前查 `isWritable()`；要么带 timeout 阻塞，要么不可写时 fast-fail 抛清晰异常。

### 🟠 `AbstractMessageHandler` catch `Exception` 然后塞进响应

"handler 抛异常别炸 channel"是好意，但同时把栈在生产排查时**埋深一层**。更糟的是它不 catch `Error`，所以 handler 里 OOM 会传播出去，`RuntimeException` 反而不会。**断层不一致**。

**修法方向**：catch 处用 WARN 级别打完整栈；响应里带异常类名（已经部分实现）；加结构化字段（request id、path）让 log → trace 关联好做。

### 🟠 没有"服务端优雅下线"信号

server shutdown 时，已接入连接被关闭。客户端视角上跟网络故障一模一样 → reconnector 立刻重试 → 又连回正在缩容的实例。没有 HTTP/2 GOAWAY 等价物。

**修法方向**：引入一个 GOAWAY 等价的消息类型。客户端收到后 `cancel(addr)`（或加重退避），不要当普通断连处理。

### 🟡 心跳流量共用 per-connection executor

`Heartbeater` 构造一个**真的** `RequestMessage`，走 `Call.asyncCall`，分配 `InvokeFuture` 等等。心跳回调跟用户 RPC 回调跑在同一个 per-connection executor 上。压力大时心跳可能饿死，导致误判"连接不健康"。

**修法方向**：心跳走专用小 executor；或者心跳干脆绕过 `InvokeFuture` 那套机器。

---

## 跟同类项目比

让你有个 sense：

- **vs Bolt（alipay/sofa-bolt）** —— Bolt 的 `UserProcessor` 抽象、地址解析、`ProtocolManager`、连接事件 API 都更完备。它的 `Protocol` 扩展点是真的 —— 多个协议版本并存
- **vs Dubbo Remoting** —— Dubbo 的传输层是真的可换的（历史上 Netty / Mina / Grizzly）。codec / transport / exchange 分层是显式的。x-remoting 的传输层 / RPC 层分层有这个志向但还没到
- **vs gRPC-Java** —— 不在一个问题空间（HTTP/2 多路复用、双向流、流控）。不公平比较；只是说一句：万一以后真要 HTTP/2 这套能力，现在"一应一答 + 长连接池"的模型增量演进不上去

总结：x-remoting 现在大概在 Bolt 0.x 那段时期的位置。Bolt 从那走到今天靠的是把抽象做扎实，不是堆 feature。

---

## 真正做得好的部分

- **重连机制（重写后）** —— per-endpoint 状态机、退避 + jitter、基于 listener 的可观测性、有上限的重试。**代码库里最好的部分**
- **lock-free `AbstractConnectionManager`** —— `ConcurrentHashMap.compute` + 两参数 `remove` + per-`Connections` 锁。设计扎实，NIO 线程不再跟别地址上的慢 connect 竞争
- **`Connections.closed` flag + 安全 `add()`** —— 把 disconnect / add 竞态窗口干净关掉
- **`InetSocketAddress` 进公开 API** —— 调用站点不用 cast 了，"只 TCP" 也显式了
- **Config builder + 校验** —— 启动时就抓出真实配置错误（负超时、零池大小），以前会漏到运行时
- **Netty 依赖拆分 + Dependabot + JDK 矩阵 CI** —— 基础卫生到位；CVE 跟踪和 JDK 兼容回归会被自动抓
- **文档深度（`docs/`、wiki）** —— 对这种 1k LOC 量级的项目来说，文档密度高得不正常。**大部分同体量项目都没这种深度**

---

## 推荐还债顺序

如果有人想开始还这笔债：

1. ~~**拆解 `Connection`**~~ ✅ 已完成 —— `InFlightRequests` + `HeartbeatState`
   已提取到 RPC 层作为 channel attribute。
2. ~~**删掉或真做 `Protocol`**~~ ✅ 已完成 —— `Protocol` 现在是最小接口；
   RPC 特有类型已移至 `core/rpc`。
3. **重命名误导的线程池** —— 5 分钟，立即的运维赢面
4. **`SECURITY.md` + Hessian 风险文档** —— 网络框架的安全最低线
5. **`CallOptions` builder + `IDGenerator` per-client `AtomicLong`** —— "有时间就改" 那批的剩余
6. **折叠 `MessageType` 层级** —— 已隔离在 `rpc` 包中，过度继承更容易简化了
7. **写路径背压** —— runtime 正确性单项影响最大

---

> [📖 索引](README.zh-CN.md) · 上一篇：[← FAQ](faq.zh-CN.md) · 下一篇：[路线图 →](roadmap.zh-CN.md)
