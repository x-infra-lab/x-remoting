# Contributing to x-remoting

Thank you for considering contributing to x-remoting! This guide covers
everything you need to get started.

## Development Environment

### Prerequisites

- **JDK 8+** (the CI matrix tests 8, 11, 17, and 21)
- **Maven 3.6+**
- **Git**

### Build & Test

```bash
# Full build + tests + format check + coverage
mvn clean install

# Just compile
mvn compile

# Run tests only
mvn test

# Fix code formatting
mvn spring-javaformat:apply
```

### Code Style

This project uses [Spring Java Format](https://github.com/spring-io/spring-javaformat).
The build will fail if formatting violations are detected.

Before committing, run:

```bash
mvn spring-javaformat:apply
```

### Coverage

JaCoCo enforces a 60% complexity coverage floor. The build fails if coverage
drops below this threshold.

### Performance Benchmarks

JMH benchmarks live in `src/test/java/.../benchmark/`. They are **not** part of
the normal test suite. To run them manually:

```bash
mvn clean package -DskipTests
java -cp target/test-classes:target/classes:$(mvn dependency:build-classpath -q -DincludeScope=test -Dmdep.outputFile=/dev/stdout) \
  io.github.xinfra.lab.remoting.benchmark.RpcBenchmark
```

## Making Changes

### Branch Workflow

1. Fork the repository and clone your fork.
2. Create a feature branch from `main`.
3. Make your changes in small, focused commits.
4. Run `mvn clean install` to verify everything passes.
5. Push your branch and open a pull request.

### Commit Messages

Use concise, imperative-mood messages:

```
fix: correct heartbeat timeout calculation
feat: add GOAWAY graceful shutdown
refactor: convert ServerConfig to immutable builder
docs: update getting-started examples
```

### Pull Request Process

1. Fill in the PR template.
2. Ensure the CI matrix is green (JDK 8/11/17/21).
3. Keep PRs focused — one logical change per PR.
4. Update documentation if your change affects the public API or user-facing
   behavior.

## Project Structure

```
src/main/java/io/github/xinfra/lab/remoting/
  connection/    # Connection management, reconnect, pooling
  server/        # Server bootstrap, config
  protocol/      # Protocol abstraction
  codec/         # Encode/decode pipeline
  rpc/           # RPC layer (client, server, messages, handlers)
  serialization/ # Serialization SPI

src/test/java/   # Tests mirror the main source layout
docs/            # Documentation (English + Chinese)
```

## Reporting Issues

- **Bugs**: use the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md).
- **Features**: use the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md).
- **Security**: see [SECURITY.md](SECURITY.md) — do not open public issues.

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md).
