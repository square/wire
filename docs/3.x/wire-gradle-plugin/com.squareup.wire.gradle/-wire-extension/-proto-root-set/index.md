//[wire-gradle-plugin](../../../../index.md)/[com.squareup.wire.gradle](../../index.md)/[WireExtension](../index.md)/[ProtoRootSet](index.md)

# ProtoRootSet

[jvm]\
open class [ProtoRootSet](index.md)

## Functions

| Name | Summary |
|---|---|
| [exclude](exclude.md) | [jvm]<br>fun [exclude](exclude.md)(vararg excludePaths: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [include](include.md) | [jvm]<br>fun [include](include.md)(vararg includePaths: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [srcDir](src-dir.md) | [jvm]<br>fun [srcDir](src-dir.md)(dir: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [srcDirs](src-dirs.md) | [jvm]<br>fun [srcDirs](src-dirs.md)(vararg dirs: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)) |
| [srcJar](src-jar.md) | [jvm]<br>fun [srcJar](src-jar.md)(jar: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>fun [srcJar](src-jar.md)(convertible: ProviderConvertible&lt;MinimalExternalModuleDependency&gt;)<br>fun [srcJar](src-jar.md)(provider: Provider&lt;MinimalExternalModuleDependency&gt;) |
| [srcProject](src-project.md) | [jvm]<br>fun [srcProject](src-project.md)(projectPath: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html))<br>fun [srcProject](src-project.md)(project: DelegatingProjectDependency) |

## Properties

| Name | Summary |
|---|---|
| [excludes](excludes.md) | [jvm]<br>val [excludes](excludes.md): [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [includes](includes.md) | [jvm]<br>val [includes](includes.md): [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [srcDirs](src-dirs.md) | [jvm]<br>val [srcDirs](src-dirs.md): [MutableList](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [srcJar](src-jar.md) | [jvm]<br>var [srcJar](src-jar.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [srcJarAsExternalModuleDependency](src-jar-as-external-module-dependency.md) | [jvm]<br>var [srcJarAsExternalModuleDependency](src-jar-as-external-module-dependency.md): Provider&lt;MinimalExternalModuleDependency&gt;? = null |
| [srcProject](src-project.md) | [jvm]<br>var [srcProject](src-project.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [srcProjectDependency](src-project-dependency.md) | [jvm]<br>var [srcProjectDependency](src-project-dependency.md): DelegatingProjectDependency? = null |
