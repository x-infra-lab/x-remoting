# 架构

> [📖 索引](README.zh-CN.md) · 上一篇：[← 快速开始](getting-started.zh-CN.md) · 下一篇：[RPC 使用 →](rpc-usage.zh-CN.md) · [🇬🇧 English](architecture.md)

x-remoting 由两层薄薄的代码构成：

- **api** —— 框架本身（连接 / 协议 / 事件 / 心跳 / 重连的抽象与 Netty 默认实现）
- **core** —— 基于框架实现的完整 RPC（handler 注册、request id 生成、四种调用模式、消息工厂）。`all` 模块是把两个一起打包的便利 uber-jar。

```
                            ┌────────────────────┐
                            │   你的应用代码      │
                            └─────────┬──────────┘
                                      │
   ┌──────────────────────────────────┴──────────────────────────────────┐
   │                                                                     │
   │                          x-remoting-core                            │
   │   RemotingClient / RemotingServer  (blockingCall / futureCall /     │
   │                                     asyncCall / oneway)             │
   │   RequestHandler / RequestApi / RemotingProtocol / Codec            │
   │                                                                     │
   └──────────────────────────────────┬──────────────────────────────────┘
                                      │
   ┌──────────────────────────────────┴──────────────────────────────────┐
   │                                                                     │
   │                          x-remoting-api                             │
   │   ConnectionManager     ConnectionFactory     Protocol              │
   │   Reconnector           Heartbeater           MessageHandler        │
   │   ConnectionEventProcessor / Listener                               │
   │   AbstractServer        ServerConnectionManager                     │
   │                                                                     │
   └──────────────────────────────────┬──────────────────────────────────┘
                                      │
                                ┌─────┴──────┐
                                │   Netty    │
                                └────────────┘
```

## 模块

| 模块 | 坐标 | 内容 |
|------|------|------|
| `api`  | `io.github.x-infra-lab:x-remoting-api` | 框架抽象 + Netty 默认实现 |
| `core` | `io.github.x-infra-lab:x-remoting-core` | RPC 实现（`impl.client` / `impl.server` / `impl.handler` / `impl.codec` / `impl.message`） |
| `all`  | `io.github.x-infra-lab:x-remoting` | 便利 uber-jar，依赖前两者 |

只想要框架自己（拿来写自己的协议）→ 依赖 `api`；想要开箱即用的 RPC → 依赖 `all`（或 `core`）。

> 提醒：`api` / `core` 的分层目前更多是理想而非强制。诚实评价见[设计债](design-debt.zh-CN.md)。

## 关键抽象

| 类型 | 层 | 职责 |
|------|----|------|
| `Protocol` | api | 线协议门面：codec、消息工厂、消息 handler |
| `Connection` | api | 包一个 Netty `Channel` + per-connection executor/timer + 未完成 `InvokeFuture` map |
| `Connections` | api | 单个地址的连接池，`close` 后 `add` 不会泄漏 |
| `ConnectionFactory` | api | 给一个地址构造 `Connection` |
| `ConnectionManager` | api | get-or-connect、disconnect、close —— 持有 `connectionsMap` |
| `ConnectionEventProcessor` | api | 异步分发 `CONNECT` / `CLOSE` 事件给监听器 |
| `Heartbeater` | api | 发心跳、计数失败、关链路 |
| `Reconnector` | api | 每个 endpoint 一个的重连状态机，带退避 + 监听器 |
| `Server` / `AbstractServer` | api | bind、accept、转给 manager |
| `RemotingProtocol` | core | 内置 RPC 的具体 `Protocol` |
| `RemotingClient` / `RemotingServer` | core | RPC 入口 —— 大部分用户直接接触的就是这两个 |
| `RequestHandler` / `RequestApi` / `RequestHandlerRegistry` | core | 服务端按 path 派发 |
| `CallOptions` / `RemotingCallBack` / `RemotingFuture` | core | per-call 配置 + 异步结果类型 |

## 一次请求的流转

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                  客户端                                       │
│                                                                               │
│  RemotingClient.blockingCall(api, request, addr, opts)                        │
│            │                                                                  │
│            ▼                                                                  │
│  RemotingCall ── buildRequestMessage（id、序列化、headers） ──┐                │
│            │                                                    │             │
│            ▼                                                    ▼             │
│  ConnectionManager.get(addr) ────────────────────┐    追踪 InvokeFuture       │
│            │  （池命中，或 connect + 填到 N 条） │                            │
│            ▼                                     │                            │
│  Connection ── pipeline.writeAndFlush ───────────┘                            │
│            │                                                                  │
└────────────┼──────────────────────────────────────────────────────────────────┘
             │
             ▼  TCP，Hessian 编码的长度前缀帧
┌────────────┼──────────────────────────────────────────────────────────────────┐
│            │                       服务端                                     │
│            ▼                                                                  │
│  Netty pipeline: ProtocolDecoder → ProtocolHandler → RemotingMessageHandler   │
│            │                                                                  │
│            ▼                                                                  │
│  RemotingRequestMessageTypeHandler                                            │
│            │                                                                  │
│            ▼                                                                  │
│  RequestHandlerRegistry.lookup(path) → 用户的 RequestHandler.handle(request)  │
│            │                                                                  │
│            ▼                                                                  │
│  ResponseObserver.complete(result) → Responses.sendResponse                   │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
                       │  TCP 回程
                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  客户端 pipeline → ResponseMessageTypeHandler → InvokeFuture.complete()      │
│  RemotingFuture.get() / callback 触发 / blockingCall 返回                    │
└──────────────────────────────────────────────────────────────────────────────┘
```

## 线程模型

| 线程组 | 归属 | 默认大小 | 用途 |
|--------|------|----------|------|
| `RemotingClient-Client-IO-Worker-*` | `DefaultConnectionFactory` | `availableProcessors()` | 出站 channel 的 Netty NIO/Epoll worker |
| `RemotingServer-IO-Boss` | `AbstractServer` | 1 | accept 线程 |
| `RemotingServer-IO-Worker-*` | `AbstractServer` | `availableProcessors() * 2` | 入站 channel 的 Netty NIO/Epoll worker |
| `RemotingClient-Client-Default-Executor-*` | `DefaultConnectionFactory` | `availableProcessors()` | per-connection 回调（如 `RemotingCallBack`） |
| `RemotingClient-Server-Default-Executor-*` | `AbstractServer` | cached 池 | 服务端 `RequestHandler` 执行池 |
| `RemotingClient-Client-Timer` / `RemotingClient-Server-Timer` | factory/server | 1 | 请求超时用的 `HashedWheelTimer` |
| `RemotingClient-Connection-Event` | `DefaultConnectionEventProcessor` | 1 | 把 `CONNECT` / `CLOSE` 事件分发给监听器 |
| `RemotingClient-Reconnect-Timer` | `DefaultReconnector` | 1 | 排期重连尝试的 `HashedWheelTimer` |
| `RemotingClient-Reconnect-Worker-*` | `DefaultReconnector` | 4（可配） | 跑重连阻塞 `connect()` 的线程池 |

热路径上**没有全局锁**。`ConnectionManager` 用 `ConcurrentHashMap.compute` + per-endpoint `Connections` 锁；reconnector 每个 endpoint 一个小锁；listener 可以指定自己的 executor 避免阻塞 dispatcher。细节见 [ConnectionManager](connection-manager.zh-CN.md)。

## 公开 API 约定

所有跟 endpoint 相关的 API（`ConnectionManager` / `Reconnector` / `Heartbeater` / 客户端/服务端 call 方法）入参出参都是 **`InetSocketAddress`**。内部的 Netty channel API 还是 `SocketAddress`；`Connection.remoteAddress()` 返回 `SocketAddress`，在框架边界做 cast。

所有 `*Config` 类都是 immutable + builder。`build()` 校验入参，非法时抛 `IllegalArgumentException`。详见[配置参考](configuration-reference.zh-CN.md)。

---

> [📖 索引](README.zh-CN.md) · 上一篇：[← 快速开始](getting-started.zh-CN.md) · 下一篇：[RPC 使用 →](rpc-usage.zh-CN.md)
