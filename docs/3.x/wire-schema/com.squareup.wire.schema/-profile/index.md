//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[Profile](index.md)

# Profile

[common]\
class [Profile](index.md)(profileFiles: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ProfileFileElement](../../com.squareup.wire.schema.internal/-profile-file-element/index.md)&gt;)

[js]\
class [Profile](index.md)(profileFiles: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;ProfileFileElement&gt;)

[jvm]\
class [Profile](index.md)(profileFiles: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;ProfileFileElement&gt;)

Describes how to map .proto to .java. A single repository of .proto files may have multiple profiles; for example a project may target both Android and Java.

## Constructors

| | |
|---|---|
| [Profile](-profile.md) | [common]<br>fun [Profile](-profile.md)(profileFiles: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[ProfileFileElement](../../com.squareup.wire.schema.internal/-profile-file-element/index.md)&gt; = emptyList())<br>[js, jvm]<br>fun [Profile](-profile.md)(profileFiles: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;ProfileFileElement&gt; = emptyList()) |

## Functions

| Name | Summary |
|---|---|
| [getAdapter](get-adapter.md) | [jvm]<br>fun [getAdapter](get-adapter.md)(type: ProtoType): [AdapterConstant](../-adapter-constant/index.md)? |
| [javaTarget](java-target.md) | [jvm]<br>fun [javaTarget](java-target.md)(type: ProtoType): TypeName? |
| [kotlinTarget](kotlin-target.md) | [jvm]<br>fun [kotlinTarget](kotlin-target.md)(type: ProtoType): TypeName? |
