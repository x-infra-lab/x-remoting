# RPC 使用

> [📖 索引](README.zh-CN.md) · 上一篇：[← 架构](architecture.zh-CN.md) · 下一篇：[配置参考 →](configuration-reference.zh-CN.md) · [🇬🇧 English](rpc-usage.md)

RPC 层（`rpc.*` 包）带了一套完整的 RPC。本页介绍你日常会用到的入口 API。

## RemotingClient

```java
RemotingClient client = new RemotingClient();         // 默认配置
// 或：
RemotingClient client = new RemotingClient(clientConfig);

client.startup();
// ... 调用 ...
client.shutdown();
```

无参构造器用的是 `ConnectionFactoryConfig.defaults()` / `ConnectionManagerConfig.defaults()` / `ReconnectConfig.defaults()`。可配项见[配置参考](configuration-reference.zh-CN.md)。

### 四种调用模式

每种调用前四个参数都一样：`(RequestApi, request, InetSocketAddress, CallOptions)`。区别只在响应怎么交付。

#### `blockingCall` —— 同步等

```java
String result = client.blockingCall(
        RequestApi.of("echo"),
        new EchoRequest("hi"),
        address,
        CallOptions.defaults());
```

线程阻塞直到响应到达或 per-call 超时（`CallOptions.timeoutMills`，默认 3000ms）。传输/序列化错误抛 `RemotingException`，服务端错误响应抛 `ResponseStatusRuntimeException`。

#### `futureCall` —— 拿 `RemotingFuture<R>`

```java
RemotingFuture<String> future = client.futureCall(
        RequestApi.of("echo"),
        new EchoRequest("hi"),
        address,
        CallOptions.defaults());

String result = future.get(3, TimeUnit.SECONDS);   // 或 future.get()
boolean done = future.isDone();
```

#### `asyncCall` —— 注册回调

```java
client.asyncCall(
        RequestApi.of("echo"),
        new EchoRequest("hi"),
        address,
        CallOptions.defaults(),
        new RemotingCallBack<String>() {
            @Override public void onResponse(String response) { /* ... */ }
            @Override public void onException(Throwable t)   { /* ... */ }
        });
```

回调跑在连接的 executor（`RemotingClient-Client-Default-Executor-*`）上，**不要在这线程里阻塞**。

#### `oneway` —— 发完不管

```java
client.oneway(RequestApi.of("notify"), new Notify("..."), address, CallOptions.defaults());
```

不返回响应、不追踪 `InvokeFuture`。适合不关心对端是否收到的事件型场景。

## RemotingServer

```java
RemotingServerConfig serverConfig = new RemotingServerConfig();
serverConfig.setPort(8989);

RemotingServer server = new RemotingServer(serverConfig);

server.registerRequestHandler(
        RequestApi.of("echo"),
        (EchoRequest req) -> "echo: " + req.getMessage());

server.startup();
```

### 异步 handler

`RequestHandler<T, R>` 有一个 default 的 `asyncHandle(request, ResponseObserver<R>)`，允许稍后完成。要非阻塞处理就覆盖它：

```java
server.registerRequestHandler(RequestApi.of("slow"), new RequestHandler<SlowReq, String>() {
    @Override public String handle(SlowReq req) { throw new UnsupportedOperationException(); }

    @Override public void asyncHandle(SlowReq req, ResponseObserver<String> obs) {
        someService.fetchAsync(req).whenComplete((res, err) -> {
            if (err != null) obs.onError(err);
            else             obs.complete(res);
        });
    }

    @Override public Executor getExecutor() { return myAppExecutor; }
});
```

handler 的 `getExecutor()` 返回非空时，框架会把 handler 调用派发到这个 executor，不再用服务端默认的。

### 服务端反向调用

`RemotingServer` 暴露的 call 方法跟 client 一模一样（`blockingCall` / `futureCall` / `asyncCall` / `oneway`）。客户端连上来之后，服务端可以反过来给该客户端 push 请求：

```java
RemotingServerConfig serverConfig = new RemotingServerConfig();
serverConfig.setPort(8989);
serverConfig.setManageConnection(true);          // ← 反向调用必须开
RemotingServer server = new RemotingServer(serverConfig);

// 之后拿到客户端的 InetSocketAddress...
server.oneway(RequestApi.of("notify"), new Notify("hello"), clientAddress, callOptions);
```

`manageConnection=true` 让服务端把入站连接留在 `ServerConnectionManager` 里。不开就只能收请求，不能主动发。

客户端要给服务端可能 push 的 path 注册 handler：

```java
client.registerRequestHandler(RequestApi.of("notify"), (Notify n) -> { /* ... */ });
```

## CallOptions

```java
CallOptions opts = CallOptions.builder()
        .timeoutMills(5000)
        .serializationType(SerializationType.Hession)
        .build();

// 或者用全部默认值：
CallOptions opts = CallOptions.defaults();
```

- `timeoutMills` —— per-call 超时。框架往 `HashedWheelTimer` 排个 timeout，到点没拿到响应就用 `Timeout` 状态完成 `InvokeFuture`。
- `serializationType` —— 当前只发了 `Hession`；SPI 是 enum 形态，结构上开放。
- `headers` —— 请求消息上挂的自定义字符串 header，服务端 handler 能读到。

## 连接生命周期（RPC 视角）

```
client.startup()
   └── ClientConnectionManager.startup()
          ├── ConnectionEventProcessor.startup()
          └── Reconnector.startup()        // timer + worker 池

（第一次调用地址 A）
   └── ConnectionManager.get(A)
          ├── 新建 Connection（Netty bootstrap.connect）
          ├── 加进 connectionsMap[A]
          └── fire ConnectionEvent.CONNECT  ──▶ 监听器

（channel 掉线）
   └── Netty channelInactive ─▶ ConnectionManager.close(conn)
          ├── 把 conn 从 connectionsMap[A] 里 invalidate
          ├── fire ConnectionEvent.CLOSE     ──▶ 监听器
          └── reconnector.onUnhealthy(A)
                 └── 按 backoff 排期重连 …

client.shutdown()
   └── ClientConnectionManager.shutdown()
          ├── 断开所有地址
          ├── connectionFactory.close()   // 关闭 Netty 线程组
          └── reconnector.shutdown()      // 取消 timer，等 worker 退出
```

细节见 [ConnectionManager](connection-manager.zh-CN.md) 和[重连](reconnect.zh-CN.md)。

## 常见坑

- **反向调用忘了开 `manageConnection=true`**：服务端没存连接 → 抛 `RemotingException`。
- **`RemotingCallBack` 里阻塞**：跑在客户端 I/O executor 上，做任何稍微重一点的事情都要先 trampoline 到自己的池。
- **`EchoRequest` 这种 POJO 跨版本演进时 Hessian 兼容性没注意**：Hessian 基于字段名，加字段要谨慎。
- **shutdown 调两次**：`AbstractLifeCycle` 有 CAS 守卫，第二次会抛 `IllegalStateException`。

---

> [📖 索引](README.zh-CN.md) · 上一篇：[← 架构](architecture.zh-CN.md) · 下一篇：[配置参考 →](configuration-reference.zh-CN.md)
