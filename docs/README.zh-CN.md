# x-remoting 文档

> [🇬🇧 English](README.md) | 🇨🇳 中文

基于 Netty 的网络通信框架。x-remoting 的初衷是让中间件开发者不必为连接管理、编解码、心跳和重连重复造轮子，把精力放在业务本身。

[![Action Status](https://github.com/x-infra-lab/x-remoting/actions/workflows/maven-build.yml/badge.svg)](https://github.com/x-infra-lab/x-remoting/actions)
[![codecov](https://codecov.io/gh/x-infra-lab/x-remoting/graph/badge.svg)](https://codecov.io/gh/x-infra-lab/x-remoting)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.x-infra-lab/x-remoting)](https://central.sonatype.com/artifact/io.github.x-infra-lab/x-remoting/)

## 目录

按顺序阅读，或直接跳到你需要的章节。

| # | 文档 | 主要内容 |
|---|------|--------|
| 1 | [快速开始](getting-started.zh-CN.md) | 安装、hello world、第一个请求 |
| 2 | [架构](architecture.zh-CN.md) | 模块划分、关键抽象、请求流转、线程模型 |
| 3 | [RPC 使用](rpc-usage.zh-CN.md) | 四种调用模式、Handler、服务端反向调用 |
| 4 | [配置参考](configuration-reference.zh-CN.md) | 全部配置项 |
| 5 | [ConnectionManager](connection-manager.zh-CN.md) | 连接池、生命周期、事件、心跳 |
| 6 | [重连机制](reconnect.zh-CN.md) | 状态机、退避策略、监听器 |
| 7 | [扩展](extending.zh-CN.md) | 自定义退避、监听器、协议 |
| 8 | [FAQ](faq.zh-CN.md) | 常见问题和陷阱 |
| 9 | [设计债](design-debt.zh-CN.md) | 留给贡献者的架构债清单 |
| 10 | [路线图](roadmap.zh-CN.md) | 通往 1.0 的项目路线图 |

## 项目状态

x-remoting 当前处于 0.x 预览阶段 —— 小版本之间 API 可能调整。最低支持 JDK 8；CI 矩阵覆盖 8 / 11 / 17 / 21。

## License

Apache License, Version 2.0. 见 [LICENSE](../LICENSE)。
