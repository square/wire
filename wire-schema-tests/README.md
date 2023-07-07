Wire Schema Tests
===============

This module contains helper methods to test [schema handlers][schemaHandlers], and some
[recipes][recipes].

SchemaBuilder
-------------

To load and build a schema in memory:

```kotlin
val schema = buildSchema {
  add(
    name = "test/message.proto".toPath(),
    protoFile = """
        |syntax = "proto2";
        |
        |package test;
        |
        |message Request {}
        |message Response {
        |  optional string result = 1;
        |}
      """.trimMargin()
  )
  add(
    name = "test/service.proto".toPath(),
    protoFile = """
        |syntax = "proto2";
        |
        |package test;
        |
        |import "test/message.proto";
        |
        |service MyService {
        |  rpc fetch(test.Request) returns(test.Response) {};
        |}
      """.trimMargin()
  )
}
```

You can then test your schema handlers:

```kotlin
class LogToFileHandler : SchemaHandler() {
  private val filePath = "log.txt".toPath()

  override fun handle(type: Type, context: SchemaHandler.Context): Path? {
    context.fileSystem.appendingSink(filePath).buffer().use {
      it.writeUtf8("Generating type: ${type.type}\n")
    }

    return null
  }

  override fun handle(service: Service, context: SchemaHandler.Context): List<Path> {
    context.fileSystem.appendingSink(filePath).buffer().use {
      it.writeUtf8("Generating service: ${service.type}\n")
    }

    return listOf()
  }

  override fun handle(extend: Extend, field: Field, context: SchemaHandler.Context): Path? {
    context.fileSystem.appendingSink(filePath).buffer().use {
      it.writeUtf8("Generating ${extend.type} on ${field.location}\n")
    }

    return null
  }
}

val context = SchemaHandler.Context(
  fileSystem = FakeFileSystem(),
  outDirectory = "/".toPath(),
  logger = WireTestLogger(),
  sourcePathPaths = setOf("test/message.proto", "test/service.proto"),
)
LogToFileHandler().handle(schema, context)

val content = context.fileSystem.read("log.txt".toPath(), BufferedSource::readUtf8)
val expected = """
    |Generating type: test.Request
    |Generating type: test.Response
    |Generating service: test.MyService
    |""".trimMargin()
assertEquals(expected, content)
```

With Java, building a schema would look like:
```java
Schema schema = new SchemaBuilder()
  .add(Path.get("message.proto"), ""
      + "message Message {\n"
      + "  required float long = 1;\n"
      + "}\n")
  .build();
```

[schemaHandlers]: https://square.github.io/wire/wire_compiler/#custom-handlers
[recipes]: https://github.com/square/wire/tree/master/wire-schema-tests/src/test/java/com/squareup/wire/recipes
