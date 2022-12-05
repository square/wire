//[wire-compiler](../../../index.md)/[com.squareup.wire.schema](../index.md)/[WireRun](index.md)

# WireRun

[jvm]\
data class [WireRun](index.md)(sourcePath: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Location&gt;, protoPath: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Location&gt;, treeShakingRoots: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, treeShakingRubbish: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, moves: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;TypeMover.Move&gt;, sinceVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, untilVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, onlyVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?, targets: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Target](../-target/index.md)&gt;, modules: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [WireRun.Module](-module/index.md)&gt;, permitPackageCycles: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html))

An invocation of the Wire compiler. Each invocation performs the following operations:

<ol><li>Read source .proto files directly from the file system or from archive files (ie. .jar     and .zip files). This will also load imported .proto files from either the [sourcePath](source-path.md)     or [protoPath](proto-path.md). The collection of loaded type declarations is called a schema.</li><li>Validate the schema and resolve references between types.</li><li>Optionally refactor the schema. This builds a new schema that is a subset of the original.     The new schema contains only types that are both transitively reachable from     [treeShakingRoots](tree-shaking-roots.md) and not in [treeShakingRubbish](tree-shaking-rubbish.md). Types are moved to different files as     specified by [moves](moves.md).</li><li>Call each target. It will generate sources for protos in the [sourcePath](source-path.md) that are in its     [Target.includes](../-target/includes.md), that are not in its [Target.excludes](../-target/excludes.md), and that haven't already been     emitted by an earlier target.</li></ol>

Source Directories and Archives

 -------------------------------

The [sourcePath](source-path.md) and [protoPath](proto-path.md) lists contain locations that are of the following forms:

<ul><li>Locations of .proto files.</li><li>Locations of directories that contain a tree of .proto files. Typically this is a directory     ending in src/main/proto.</li><li>Locations of .zip and .jar archives that contain a tree of .proto files. Typically this     is a .jar file from a Maven repository.</li></ul>

When one .proto message imports another, the import is resolved from the base of each location and archive. If the build is in the unfortunate situation where an import could be resolved by multiple files, whichever was listed first takes precedence.

Although the content and structure of [sourcePath](source-path.md) and [protoPath](proto-path.md) are the same, only types defined in [sourcePath](source-path.md) are used to generate sources.

Matching Packages, Types, and Members

 -------------------------------------

The [treeShakingRoots](tree-shaking-roots.md), [treeShakingRubbish](tree-shaking-rubbish.md), [Target.includes](../-target/includes.md) and [Target.excludes](../-target/excludes.md) lists contain strings that select proto types and members. Strings in these lists are in one of these forms:

<ul><li>Package names followed by .*, like squareup.dinosaurs.*. This matches types defined in the     package and its descendant packages. A lone asterisk * matches all packages.</li><li>Fully-qualified type names like squareup.dinosaurs.Dinosaur. Types may be messages, enums,     or services.</li><li>Fully-qualified member names like squareup.dinosaurs.Dinosaur#name. These are type names     followed by # followed by a member name. Members may be message fields, enum constants, or     service RPCs.</li></ul>

It is an error to specify mutually-redundant values in any of these lists. For example, the list [squareup.dinosaurs, squareup.dinosaurs.Dinosaur] is invalid because the second element is already matched by the first.

Every element in each lists must apply to at least one declaration. Otherwise that option is unnecessary and a possible typo.

Composability

 -------------

There are many moving parts in this system! For most applications it is safe to use [sourcePath](source-path.md) and [targets](targets.md) only. The other options are for the benefit of large and modular applications.

###  Use [protoPath](proto-path.md) when one proto module depends on another proto module.

These .proto files are used for checking dependencies only. It is assumed that the sources for these protos are generated elsewhere.

###  Use tree shaking to remove unwanted types.

[Tree shaking](https://en.wikipedia.org/wiki/Tree_shaking) can be used to create a small-as-possible generated footprint even if the source declarations are large. This works like [ProGuard](https://en.wikipedia.org/wiki/ProGuard_(software)) and other code shrinking compilers: it allows you to benefit from a shared codebase without creating a large artifact.

###  Use multiple targets to split generated code across multiple programming languages.

If your project is already using generated Java, it’s difficult to switch to generated Kotlin. Instead of switching everything over at once you can use multiple targets to switch over incrementally. Targets consume their types; subsequent targets get whatever types are left over.

## Constructors

| | |
|---|---|
| [WireRun](-wire-run.md) | [jvm]<br>fun [WireRun](-wire-run.md)(sourcePath: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Location&gt;, protoPath: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Location&gt; = listOf(), treeShakingRoots: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf("*"), treeShakingRubbish: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = listOf(), moves: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;TypeMover.Move&gt; = listOf(), sinceVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, untilVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, onlyVersion: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, targets: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Target](../-target/index.md)&gt;, modules: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [WireRun.Module](-module/index.md)&gt; = emptyMap(), permitPackageCycles: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) |

## Types

| Name | Summary |
|---|---|
| [Module](-module/index.md) | [jvm]<br>data class [Module](-module/index.md)(dependencies: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, pruningRules: PruningRules?) |

## Functions

| Name | Summary |
|---|---|
| [execute](execute.md) | [jvm]<br>fun [execute](execute.md)(fs: FileSystem = FileSystem.SYSTEM, logger: WireLogger = ConsoleWireLogger()) |

## Properties

| Name | Summary |
|---|---|
| [modules](modules.md) | [jvm]<br>val [modules](modules.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [WireRun.Module](-module/index.md)&gt;<br>A map from module dir to module info which dictates how the loaded types are partitioned and generated. |
| [moves](moves.md) | [jvm]<br>val [moves](moves.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;TypeMover.Move&gt;<br>Types to move before generating code or producing other output. Use this with [ProtoTarget](../-proto-target/index.md) to refactor proto schemas safely. |
| [onlyVersion](only-version.md) | [jvm]<br>val [onlyVersion](only-version.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>The only version of the version range. Fields with until values greater than this, as well as fields with since values less than or equal to this, are retained. This field is mutually exclusive with sinceVersion and untilVersion. |
| [permitPackageCycles](permit-package-cycles.md) | [jvm]<br>val [permitPackageCycles](permit-package-cycles.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false<br>If true, no validation will be executed to check package cycles. |
| [protoPath](proto-path.md) | [jvm]<br>val [protoPath](proto-path.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Location&gt;<br>Sources .proto files for this task to use when resolving references. |
| [sinceVersion](since-version.md) | [jvm]<br>val [sinceVersion](since-version.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>The exclusive lower bound of the version range. Fields with until values greater than this are retained. |
| [sourcePath](source-path.md) | [jvm]<br>val [sourcePath](source-path.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;Location&gt;<br>Source .proto files for this task to generate from. |
| [targets](targets.md) | [jvm]<br>val [targets](targets.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Target](../-target/index.md)&gt;<br>Action to take with the loaded, resolved, and possibly-pruned schema. |
| [treeShakingRoots](tree-shaking-roots.md) | [jvm]<br>val [treeShakingRoots](tree-shaking-roots.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>The roots of the schema model. Wire will prune the schema model to only include types in this list and the types transitively required by them. |
| [treeShakingRubbish](tree-shaking-rubbish.md) | [jvm]<br>val [treeShakingRubbish](tree-shaking-rubbish.md): [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;<br>Types and members that will be stripped from the schema model. Wire will remove the elements themselves and also all references to them. |
| [untilVersion](until-version.md) | [jvm]<br>val [untilVersion](until-version.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null<br>The inclusive upper bound of the version range. Fields with since values less than or equal to this are retained. |
