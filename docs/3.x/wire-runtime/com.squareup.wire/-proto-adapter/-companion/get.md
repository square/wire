//[wire-runtime](../../../../index.md)/[com.squareup.wire](../../index.md)/[ProtoAdapter](../index.md)/[Companion](index.md)/[get](get.md)

# get

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun &lt;[M](get.md) : [Message](../../-message/index.md)&lt;*, *&gt;&gt; [get](get.md)(message: [M](get.md)): [ProtoAdapter](../index.md)&lt;[M](get.md)&gt;

Returns the adapter for the type of Message.

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun &lt;[M](get.md)&gt; [get](get.md)(type: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[M](get.md)&gt;): [ProtoAdapter](../index.md)&lt;[M](get.md)&gt;

Returns the adapter for type.

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun [get](get.md)(adapterString: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ProtoAdapter](../index.md)&lt;*&gt;

Returns the adapter for a given adapterString. adapterString is specified on a proto message field's WireField annotation in the form com.squareup.wire.protos.person.Person#ADAPTER.
