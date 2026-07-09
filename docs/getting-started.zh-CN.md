# 快速开始

> [📖 索引](README.zh-CN.md) · 下一篇：[架构 →](architecture.zh-CN.md) · [🇬🇧 English](getting-started.md)

本页带你从空 `pom.xml` 走到一次 server/client 完整往返，五分钟以内。

## 前置条件

- JDK 8 或更高（CI 矩阵覆盖 8/11/17/21）
- Maven 3.6+
- 能访问 Maven Central

## 1. 添加依赖

```xml
<dependency>
    <groupId>io.github.x-infra-lab</groupId>
    <artifactId>x-remoting</artifactId>
    <version><!-- Maven Central 最新版本 --></version>
</dependency>
```

[![Maven Central](https://img.shields.io/maven-central/v/io.github.x-infra-lab/x-remoting)](https://central.sonatype.com/artifact/io.github.x-infra-lab/x-remoting/)

x-remoting 依赖 `netty-handler` 与 `netty-transport-classes-epoll`。Linux 上想用 Epoll 拿最高吞吐，需要**手动**再加 native 库：

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-transport-native-epoll</artifactId>
    <classifier>linux-x86_64</classifier>
    <!-- 或 linux-aarch64 -->
</dependency>
```

没有 native 库时，x-remoting 会自动降级到 NIO。

## 2. 定义请求载荷

请求/响应对象需要走线（默认 Hessian 序列化），必须实现 `Serializable`：

```java
import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class EchoRequest implements Serializable {
    private String message;
}
```

## 3. 启动服务端

Handler 用 `RequestApi` 作为 key；客户端通过这个 path 字符串路由请求。

```java
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.server.RemotingServer;
import io.github.xinfra.lab.remoting.impl.server.RemotingServerConfig;

RemotingServerConfig serverConfig = new RemotingServerConfig();
serverConfig.setPort(8989);

RemotingServer server = new RemotingServer(serverConfig);
server.registerRequestHandler(
        RequestApi.of("echo"),
        (EchoRequest req) -> "echo: " + req.getMessage());
server.startup();

System.out.println("server listening on " + server.getLocalAddress());
```

`RequestHandler<T, R>` 只有一个抽象方法 `R handle(T request)`，可以直接写 lambda。需要非阻塞处理时覆盖 default 的 `asyncHandle(request, observer)`，稍后再调 `observer.complete(result)`。

## 4. 发送请求

```java
import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.impl.client.RemotingClient;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;

import java.net.InetSocketAddress;

RemotingClient client = new RemotingClient();
client.startup();

InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8989);
RequestApi echo = RequestApi.of("echo");

String result = client.blockingCall(
        echo,
        new EchoRequest("hello"),
        address,
        CallOptions.defaults());

System.out.println(result);   // → echo: hello
```

用完按反序关闭：

```java
client.shutdown();
server.shutdown();
```

## 这中间发生了什么

```
┌─────────────────┐                                      ┌─────────────────┐
│  RemotingClient │   ┌──── TCP (Netty) ─────────────┐   │  RemotingServer │
│                 │ ──▶ EchoRequest (Hessian 编码) ────▶│                 │
│  blockingCall() │                                       │  echo handler   │
│                 │ ◀── RemotingResponseMessage ─────── ◀ │                 │
└─────────────────┘                                      └─────────────────┘
```

x-remoting 帮你做了几件事：

1. 打开了一条到 `127.0.0.1:8989` 的池化 TCP 连接（保活给下一次调用复用）
2. 装上 `IdleStateHandler` 并开始每 15s 一次心跳
3. 通过 `RemotingProtocol` 编码请求（长度前缀帧 + Hessian payload），分配 request id，追踪 `InvokeFuture`
4. 把响应路由回 `blockingCall` 调用线程
5. 启用了一套[重连](reconnect.zh-CN.md)状态机 —— 下次调用前发生瞬时 TCP 断开会被自动处理

## 下一步

- 理解整体构成：**[架构 →](architecture.zh-CN.md)**
- 看完整四种调用模式（future / async / oneway）：[RPC 使用](rpc-usage.zh-CN.md)
- 调连接池、心跳、重连：[配置参考](configuration-reference.zh-CN.md)

---

> [📖 索引](README.zh-CN.md) · 下一篇：[架构 →](architecture.zh-CN.md)
