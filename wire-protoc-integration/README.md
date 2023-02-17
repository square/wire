Adds `protoc` integration via the `wire-protoc-integration` module.

The executables `protoc-gen-wire-java` and `protoc-gen-wire-kotlin` are called by `protoc`. The executables have a dependency on the jar output from the `wire-protoc-integration` module. The `protoc-java.jar` and `protoc-kotlin.jar` outputs generated code via the integration the protoc request and response API.

# Protoc integration and execution
To execute `protoc-gen-wire-kotlin`, you will need to install `protoc`. For more information on using the `protoc` tool, check out the [`protobuf.dev`](https://protobuf.dev/getting-started/javatutorial/#compiling-protocol-buffers) website.

To use the `protoc-gen-wire-kotlin` plugin, the following command would generate code from the `src/main/testproto/simple.proto` file:

```
protoc \
  --proto_path=./src/main/testproto \
  --plugin=$HOME/wire/wire-protoc-integration/protoc-gen-wire-kotlin \
  --wire-kotlin_out=./gen  \
  ./src/main/testproto/simple.proto
```

`--plugin` accepts the fully qualified path for the executable (`protoc-gen-wire-kotlin`).
`--wire-kotlin_out` specifies the output directory for the plugin.
(Note: if you're using `protoc-gen-wire-java` this will change to `--wire-java_out`)

# How the plugin integration works
Wire handles the compilation and linking of protobuf files along with outputting ergonomic and idiomatic code. Google's`protoc` handles compilation and linking of protobuf files along with the extensibility to provide custom generators. The module here primarily integrates `wire`'s Schema and code generation with the compilation and linking within `protoc`.

The `WireGenerator` is the primary class which mostly does data mapping from the `protoc` file descriptor to `wire`'s Schema. The input (via `System.in`) from `WireGenerator` is a `PluginProtos.CodeGeneratorRequest` which is modeled after the [`descriptor.proto`](https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/descriptor.proto) file. The output (via `System.out`) from `WireGenerator` is the `Plugin.Response`.
