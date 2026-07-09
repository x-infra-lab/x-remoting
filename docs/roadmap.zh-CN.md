# 路线图

> [📖 索引](README.zh-CN.md) · 上一篇：[← 设计债](design-debt.zh-CN.md) · [🇬🇧 English](roadmap.md)

本路线图指导 x-remoting 从当前 0.x 预览状态走向生产可用的 1.0 发布。每个阶段
建立在前一阶段的基础上；同一阶段内的条目可以独立推进（除非另有说明）。

**当前快照**（截至 `6e3b5f6`）：

| 指标 | 值 |
|------|-----|
| 版本 | 0.0.3-RC2 |
| 源码行数 | ~5 300 |
| 测试数 | 94+ |
| JaCoCo 下限 | 40% |
| CI 矩阵 | JDK 8 / 11 / 17 / 21 |
| 依赖自动化 | Dependabot（Maven + Actions，每周） |
| 文档 | 9 主题 × 2 语言 + 设计债审计 |

---

## 已完成

以下改进在设计债审计之后已经落地：

- [x] 重连机制重写（per-endpoint 状态机，退避 + jitter）
- [x] Config 类 → 不可变 builder + 校验
- [x] 公开 API 改用 `InetSocketAddress`
- [x] Netty 依赖拆分（`netty-all` → 模块化）
- [x] 心跳 `AtomicInteger` 竞态修复
- [x] 误导的线程池改名
- [x] `CallOptions` → 不可变 builder
- [x] `Connection` god object 拆解（`InFlightRequests` + `HeartbeatState`）
- [x] 传输层 / RPC 层分离（`Protocol` 最小化，`impl.*` → `rpc.*`）
- [x] 单模块合并（`api` / `core` / `all` / `examples` 合为一个）
- [x] `@AccessForTest` 注解删除
- [x] `CHANGELOG.md` 已创建

---

## Phase 0 — 开源治理

> 快速出手。无代码变更。一天之内可完成。

- [ ] **`SECURITY.md`** — 漏洞披露渠道（邮件 / GitHub Security Advisories）。明确
  声明默认 Hessian 序列化器不可用于不可信网络。
- [ ] **`CONTRIBUTING.md`** — 开发环境搭建（JDK 8+、Maven）、代码规范
  （`spring-javaformat:apply`）、PR 流程、commit 信息规范。
- [ ] **Issue 和 PR 模板**（`.github/ISSUE_TEMPLATE/`、`.github/pull_request_template.md`）：
  bug 报告、功能请求、PR 检查清单。
- [x] **`CHANGELOG.md`** — 已创建并开始跟踪变更。
- [ ] **`CODE_OF_CONDUCT.md`** — 采用 Contributor Covenant 或类似公约。

---

## Phase 1 — API 稳定（→ 0.1.0）

> 在别人开始依赖之前把公开表面做对。

### 协议扩展点 — 做成真的

`Protocol` 接口存在但只有一个实现，`AbstractMessageHandler` 硬编码了
handler 注册。修改方向：

- [ ] **用 `MessageDispatcher` 替换 `MessageTypeHandler` 继承树** ——
  一个 `Map<MessageType, BiConsumer<Connection, Message>>` 派发器。
  每个 `Protocol` 实现注册自己的消息类型和处理器。
  删除 5 个类（`MessageTypeHandler`、`AbstractMessageHandler`、
  `AbstractRequestMessageTypeHandler`、`HeartbeatRequestMessageTypeHandler`、
  `ResponseMessageTypeHandler`），替换为 1 个。参见
  [设计债 § MessageType](design-debt.zh-CN.md#-messagetype--messagetypehandler-家族是过度继承的典型)。
- [ ] **让 `MessageType` 可扩展** — 从固定枚举改为接口，
  让自定义协议能定义自己的类型。

### 其它 API 条目

- [ ] **`IDGenerator` → per-client `AtomicLong`** — 消除 JVM 级共享、
  `Integer` 自动装箱和 2³¹ 回绕风险。
- [ ] **`RequestHandler<T, R>` SPI 清理** — 单一方法
  `void handle(T, ResponseObserver<R>)`。提供 `BlockingRequestHandler`
  适配器给同步用法。
- [ ] **`ServerConfig` → builder** — 与其他 `*Config` 类对齐。
- [ ] **`Connection.remoteAddress()`** — 增加 `inetRemoteAddress()` 直接
  返回 `InetSocketAddress`，或迁移 EmbeddedChannel 测试。
- [ ] **`RequestApi` 审视** — 要么直接收 `String`，要么升级为带版本 / 超时 /
  metadata 的类型。

---

## Phase 2 — 运行时正确性（→ 0.2.0）

> 修复在生产中会咬人的缺口。

- [ ] **写路径背压** — 写之前检查 `Channel.isWritable()`；命中高水位时
  fail-fast 并抛清晰异常。
  参见[设计债 § 写路径无背压](design-debt.zh-CN.md#-写路径无背压)。
- [ ] **服务端优雅下线（GOAWAY）** — 引入一个 GOAWAY 等价的消息类型。
  客户端收到后应 `cancel(addr)` 或加重退避，而非当作故障处理。
- [ ] **序列化 SPI** — 把 `SerializationType` 从枚举改为开放注册表。
  以 opt-in 模块形式提供 Kryo 和/或 Protobuf。文档说明 Hessian 反序列化风险。
- [ ] **异常处理一致性** — `AbstractMessageHandler`（或其替代品）应在 WARN
  级别打完整栈，响应中带异常类名，加结构化字段（request id、path）用于
  日志 → 链路关联。

---

## Phase 3 — 可观测性 & 加固（→ 0.3.0）

> 让它在生产环境里能运维。

- [ ] **Metrics SPI** — 定义 `MetricsProvider` 接口。提供 Micrometer
  和/或 OpenTelemetry 的开箱实现。关键指标：
  - 活跃连接数（gauge，per endpoint）
  - 请求延迟（histogram）
  - 重连尝试 / 成功 / 放弃（counter）
  - 心跳失败（counter）
  - 在途请求数（gauge）
- [ ] **结构化日志** — 确保 WARN+ 级别的每条日志都带 request id、path
  和 remote address。
- [ ] **测试覆盖率 → 60%+** — 重点覆盖 `Connection`、`Call`、重连和
  消息派发路径。
- [ ] **性能基准** — JMH 基准测试覆盖吞吐和延迟（sync call、async call、
  oneway），防止性能回退。

---

## Phase 4 — 1.0.0

> 稳定发版。

- [ ] **API 冻结** — 1.x 线内不做破坏性变更。没有 `@Deprecated` 标记的
  公开类即是契约。
- [ ] **迁移指南** — 文档记录从 0.x → 1.0 的每个破坏性变更，附前后代码。
- [ ] **发布到 Maven Central** — 发布非 RC 制品。
- [ ] **公告** — 博客 / README / GitHub Release Notes。

---

## 日常维护（随手做）

这些是小型独立改进，可以搭进任何 PR：

| 条目 | 严重度 | 工作量 |
|------|--------|--------|
| `Resource<T>` → `Supplier` + `Closeable` | 🟡 | S |
| 文档说明为什么保留 `Validate`（vs Commons/Guava） | 🟡 | S |
| `IDGenerator.nextRequestId()` 返回 `int` 而非 `Integer` | 🟡 | S |
| 心跳走专用小 executor | 🟡 | M |

---

## 如何认领条目

1. 查看 issue 跟踪器 — 如果还没 issue，创建一个并打上对应阶段的 label。
2. 在 issue 上评论认领。
3. 从 `main` 分支切出，实现，跑 `mvn clean install` 验证。
4. 开 PR 引用对应 issue。CI 矩阵必须全绿。

详见 [CONTRIBUTING.md](../CONTRIBUTING.md)（Phase 0 中完成）。

---

> [📖 索引](README.zh-CN.md) · 上一篇：[← 设计债](design-debt.zh-CN.md)
