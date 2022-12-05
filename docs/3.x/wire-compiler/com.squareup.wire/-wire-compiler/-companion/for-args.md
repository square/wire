//[wire-compiler](../../../../index.md)/[com.squareup.wire](../../index.md)/[WireCompiler](../index.md)/[Companion](index.md)/[forArgs](for-args.md)

# forArgs

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun [forArgs](for-args.md)(fileSystem: [FileSystem](https://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html), logger: WireLogger, vararg args: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [WireCompiler](../index.md)

@[JvmOverloads](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-overloads/index.html)

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun [forArgs](for-args.md)(fileSystem: FileSystem = FileSystem.SYSTEM, logger: WireLogger = ConsoleWireLogger(), vararg args: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [WireCompiler](../index.md)
