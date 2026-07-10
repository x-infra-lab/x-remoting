# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in x-remoting, please report it
responsibly via [GitHub Security Advisories](https://github.com/x-infra-lab/x-remoting/security/advisories/new).

Do **not** open a public issue for security vulnerabilities.

We will acknowledge receipt within 72 hours and aim to release a fix within
14 days for critical issues.

## Supported Versions

| Version   | Supported |
|-----------|-----------|
| 0.x (latest) | Yes   |

## Deserialization Security

### ClassFilter (blocklist)

All deserialization paths (`RemotingMessageBody`, `DefaultMessageHeaders`) pass
class names through `ClassFilter.loadClass()` before calling `Class.forName()`.
The filter blocks known dangerous class prefixes (e.g. `javax.management.`,
`sun.reflect.`, `com.alibaba.fastjson.`) and exact classes (`java.lang.Runtime`,
`java.lang.ProcessBuilder`, `java.lang.Thread`).

You can add custom blocked prefixes at startup:

```java
ClassFilter.addBlockedPrefix("com.example.internal.");
```

**Note:** `ClassFilter` is a blocklist, not an allowlist. It cannot cover all
possible gadget classes. For untrusted networks, use the Fury serializer with
class registration (see below).

### Fury Serializer (allowlist — recommended for untrusted networks)

`FurySerializer` runs with `requireClassRegistration(true)`. Only explicitly
registered classes can be serialized/deserialized:

```java
FurySerializer.registerClass(MyRequest.class);
FurySerializer.registerClass(MyResponse.class);
```

**Important:** register all classes before any serialization happens. Classes
registered after a thread's `Fury` instance is initialized will not be visible
to that thread.

### Hessian Serializer (default)

The default serializer (`SerializationType.Hessian`) uses the Hessian
serialization protocol. Hessian internally resolves classes during
deserialization, bypassing `ClassFilter`. The upstream library
(`com.caucho:hessian`) has had multiple deserialization RCE vulnerabilities.

**If you accept connections from untrusted peers**, use `SerializationType.Fury`
with class registration instead of Hessian.

## Scope

This policy covers the `x-remoting` library itself. Vulnerabilities in
transitive dependencies (Netty, Hessian, etc.) should be reported to their
respective upstream projects. If a transitive dependency CVE affects x-remoting
users, please open a GitHub Security Advisory so we can assess impact and
update the dependency.
