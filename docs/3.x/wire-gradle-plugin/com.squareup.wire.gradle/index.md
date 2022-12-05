//[wire-gradle-plugin](../../index.md)/[com.squareup.wire.gradle](index.md)

# Package com.squareup.wire.gradle

## Types

| Name | Summary |
|---|---|
| [CustomOutput](-custom-output/index.md) | [jvm]<br>open class [CustomOutput](-custom-output/index.md)@Injectconstructor : [WireOutput](-wire-output/index.md) |
| [InputLocation](-input-location/index.md) | [jvm]<br>class [InputLocation](-input-location/index.md) : [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html) |
| [JavaOutput](-java-output/index.md) | [jvm]<br>open class [JavaOutput](-java-output/index.md)@Injectconstructor : [WireOutput](-wire-output/index.md) |
| [KotlinOutput](-kotlin-output/index.md) | [jvm]<br>open class [KotlinOutput](-kotlin-output/index.md)@Injectconstructor : [WireOutput](-wire-output/index.md) |
| [Move](-move/index.md) | [jvm]<br>open class [Move](-move/index.md)@Injectconstructor : [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)<br>A directive to move a type to a new location and adjust all references to the type in this schema. Typically this is used with proto output to refactor a proto project. |
| [ProtoOutput](-proto-output/index.md) | [jvm]<br>open class [ProtoOutput](-proto-output/index.md)@Injectconstructor : [WireOutput](-wire-output/index.md) |
| [WireExtension](-wire-extension/index.md) | [jvm]<br>open class [WireExtension](-wire-extension/index.md)(project: Project) |
| [WireOutput](-wire-output/index.md) | [jvm]<br>abstract class [WireOutput](-wire-output/index.md)<br>Specifies Wire's outputs (expressed as a list of Target objects) using Gradle's DSL (expressed as destination directories and configuration options). This includes registering output directories with the project so they can be compiled after they are generated. |
| [WirePlugin](-wire-plugin/index.md) | [jvm]<br>class [WirePlugin](-wire-plugin/index.md) : Plugin&lt;Project&gt; |
| [WireTask](-wire-task/index.md) | [jvm]<br>@CacheableTask<br>abstract class [WireTask](-wire-task/index.md)@Injectconstructor(objects: ObjectFactory) : SourceTask |
