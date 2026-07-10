# x-remoting
![Action Status](https://github.com/x-infra-lab/x-remoting/actions/workflows/maven-build.yml/badge.svg)
[![codecov](https://codecov.io/gh/x-infra-lab/x-remoting/graph/badge.svg?token=QQUS0GPV4O)](https://codecov.io/gh/x-infra-lab/x-remoting)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.x-infra-lab/x-remoting)](https://central.sonatype.com/artifact/io.github.x-infra-lab/x-remoting/)


x-remoting is a network communication framework based on Netty.

x-remoting was born to allow middleware developers to focus more on implementing product features instead of reinventing the wheel of communication frameworks.

## Features
* Connection management (timing disconnection, automatic reconnection)
* Basic communication model (oneway, sync, future, callback)
* Heartbeat and IDLE event processing
* Customizable protocol framework
* Private protocol custom implementation - RPC communication

## Documentation

📖 **[Full documentation index](./docs/README.md)** · **[中文文档](./docs/README.zh-CN.md)**

Quick links:

* [Getting Started](./docs/getting-started.md) ([中文](./docs/getting-started.zh-CN.md))
* [Architecture](./docs/architecture.md) ([中文](./docs/architecture.zh-CN.md))
* [RPC Usage](./docs/rpc-usage.md) ([中文](./docs/rpc-usage.zh-CN.md))
* [Configuration Reference](./docs/configuration-reference.md) ([中文](./docs/configuration-reference.zh-CN.md))
* [Connection Manager](./docs/connection-manager.md) ([中文](./docs/connection-manager.zh-CN.md))
* [Reconnect](./docs/reconnect.md) ([中文](./docs/reconnect.zh-CN.md))
* [Extending](./docs/extending.md) ([中文](./docs/extending.zh-CN.md))
* [FAQ](./docs/faq.md) ([中文](./docs/faq.zh-CN.md))


## License
x-remoting is licensed under the [the Apache 2 License](https://github.com/x-infra-lab/x-remoting/blob/master/LICENSE).
