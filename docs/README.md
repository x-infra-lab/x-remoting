# x-remoting Documentation

> 🇬🇧 English | [🇨🇳 中文](README.zh-CN.md)

A network communication framework on top of Netty. x-remoting was born so middleware
developers can stop reinventing the wiring for connection management, codec,
heartbeat, and reconnect — and focus on their product.

[![Action Status](https://github.com/x-infra-lab/x-remoting/actions/workflows/maven-build.yml/badge.svg)](https://github.com/x-infra-lab/x-remoting/actions)
[![codecov](https://codecov.io/gh/x-infra-lab/x-remoting/graph/badge.svg)](https://codecov.io/gh/x-infra-lab/x-remoting)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.x-infra-lab/x-remoting)](https://central.sonatype.com/artifact/io.github.x-infra-lab/x-remoting/)

## Table of contents

Read in order, or skip to whatever you need.

| # | Document | What it covers |
|---|----------|----------------|
| 1 | [Getting Started](getting-started.md) | Install, hello world, first request |
| 2 | [Architecture](architecture.md) | Module map, key abstractions, request flow, threading |
| 3 | [RPC Usage](rpc-usage.md) | Four call patterns, handlers, server-to-client calls |
| 4 | [Configuration Reference](configuration-reference.md) | Every knob, in one place |
| 5 | [Connection Manager](connection-manager.md) | Pool, lifecycle, events, heartbeat |
| 6 | [Reconnect](reconnect.md) | State machine, backoff, listeners |
| 7 | [Extending](extending.md) | Custom backoff, listeners, protocols |
| 8 | [FAQ](faq.md) | Common questions and footguns |
| 9 | [Design Debt](design-debt.md) | Honest architectural critique for contributors |
| 10 | [Roadmap](roadmap.md) | Project roadmap toward 1.0 |

## Project status

x-remoting is currently in the 0.x preview line — APIs may change between minor
releases. JDK 8 is the supported floor; the CI matrix runs against 8 / 11 / 17 / 21.

## License

Apache License, Version 2.0. See [LICENSE](../LICENSE).
