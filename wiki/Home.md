# x-remoting

A network communication framework on top of Netty. x-remoting was born so middleware
developers can stop reinventing the wiring for connection management, codec, heartbeat,
and reconnect — and focus on their product.

[![Action Status](https://github.com/x-infra-lab/x-remoting/actions/workflows/maven-build.yml/badge.svg)](https://github.com/x-infra-lab/x-remoting/actions)
[![codecov](https://codecov.io/gh/x-infra-lab/x-remoting/graph/badge.svg)](https://codecov.io/gh/x-infra-lab/x-remoting)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.x-infra-lab/x-remoting)](https://central.sonatype.com/artifact/io.github.x-infra-lab/x-remoting/)

## What you get out of the box

- **Connection management** — pooled per-endpoint connections, timed disconnect,
  automatic reconnection with exponential backoff + jitter
- **Four call patterns** — blocking, future, async/callback, oneway
- **Heartbeat & idle handling** — driven by Netty `IdleStateEvent`, configurable
  fail-count threshold
- **Customisable protocol framework** — drop in your own wire format
- **RPC implementation** — a complete handler-based RPC built on the framework
  (`io.github.xinfra.lab.remoting.impl.*`)

## Start here

| Page | Read this when… |
|------|-----------------|
| [Getting Started](Getting-Started) | You want to send your first request |
| [Architecture](Architecture) | You want a mental model of the framework |
| [RPC Usage](RPC-Usage) | You want a deeper tour of the call APIs |
| [Connection Manager](Connection-Manager) | You need to tune pools, events, or lifecycle |
| [Reconnect](Reconnect) | You're dealing with flaky networks or failover |
| [Configuration Reference](Configuration-Reference) | You want every knob in one place |
| [Extending x-remoting](Extending-x-remoting) | You want to plug in your own backoff / listener / protocol |
| [FAQ](FAQ) | You hit a sharp edge |

## Project status

x-remoting is currently in the 0.x preview line — APIs may change between minor
releases. JDK 8 is the supported floor; the CI matrix runs against 8 / 11 / 17 / 21.

## License

Apache License, Version 2.0. See [LICENSE](https://github.com/x-infra-lab/x-remoting/blob/main/LICENSE).
