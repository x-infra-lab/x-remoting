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

## Documents
* [Getting Started](./docs/getting-started.md)


## License
x-remoting is licensed under the [the Apache 2 License](https://github.com/MicroRaft/MicroRaft/blob/master/LICENSE).
