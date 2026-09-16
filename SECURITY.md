# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 7.0.x   | ✅        |
| 6.4.x   | ✅        |
| 5.x     |           |
| 4.x     |           |
| 3.x     |           |

## Reporting a Vulnerability

Square recognizes the important contributions the security research community can make. We therefore encourage reporting security issues with the code contained in this repository.

If you believe you have discovered a security vulnerability, please follow the guidelines at https://bugcrowd.com/squareopensource.

## Threat Model

Wire generates source code from `.proto` schema files. Treat a `.proto` file as source code, in the same way you would treat a `.java`, `.kt` or `.swift` file in your build.

A schema controls the shape of the code that Wire emits. Compiling and running code generated from a schema you do not trust carries the same supply chain risk as compiling and running any other third party source code. This is not specific to Wire. The reference implementation states the same position for `protoc` in its own security policy: https://github.com/protocolbuffers/protobuf/blob/main/SECURITY.md.

In practice this means:

- Review schemas that arrive from outside your organisation, including vendored copies and schemas pulled from a shared registry, with the same care you apply to third party source code.
- Do not build or run generated code from an untrusted schema without that review.
- Prefer pinned, reviewed schema sources over schemas fetched at build time.

Wire validates schema input where validation is cheap and unambiguous. Option values that reach generated source, such as `java_package` and `wire.wire_package`, are checked before they are emitted. This validation is defence in depth. It is not a substitute for trusting the source of your schemas.

If you find a way to make Wire emit code that a schema author should not be able to produce, we want to hear about it. Report it through the channel above, whether or not it fits the guidance in this section.
