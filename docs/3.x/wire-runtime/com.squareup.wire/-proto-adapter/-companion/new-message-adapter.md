//[wire-runtime](../../../../index.md)/[com.squareup.wire](../../index.md)/[ProtoAdapter](../index.md)/[Companion](index.md)/[newMessageAdapter](new-message-adapter.md)

# newMessageAdapter

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun &lt;[M](new-message-adapter.md) : [Message](../../-message/index.md)&lt;[M](new-message-adapter.md), [B](new-message-adapter.md)&gt;, [B](new-message-adapter.md) : [Message.Builder](../../-message/-builder/index.md)&lt;[M](new-message-adapter.md), [B](new-message-adapter.md)&gt;&gt; [newMessageAdapter](new-message-adapter.md)(type: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[M](new-message-adapter.md)&gt;): [ProtoAdapter](../index.md)&lt;[M](new-message-adapter.md)&gt;

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun &lt;[M](new-message-adapter.md) : [Message](../../-message/index.md)&lt;[M](new-message-adapter.md), [B](new-message-adapter.md)&gt;, [B](new-message-adapter.md) : [Message.Builder](../../-message/-builder/index.md)&lt;[M](new-message-adapter.md), [B](new-message-adapter.md)&gt;&gt; [newMessageAdapter](new-message-adapter.md)(type: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[M](new-message-adapter.md)&gt;, typeUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ProtoAdapter](../index.md)&lt;[M](new-message-adapter.md)&gt;

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

fun &lt;[M](new-message-adapter.md) : [Message](../../-message/index.md)&lt;[M](new-message-adapter.md), [B](new-message-adapter.md)&gt;, [B](new-message-adapter.md) : [Message.Builder](../../-message/-builder/index.md)&lt;[M](new-message-adapter.md), [B](new-message-adapter.md)&gt;&gt; [newMessageAdapter](new-message-adapter.md)(type: [Class](https://docs.oracle.com/javase/8/docs/api/java/lang/Class.html)&lt;[M](new-message-adapter.md)&gt;, typeUrl: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), syntax: Syntax): [ProtoAdapter](../index.md)&lt;[M](new-message-adapter.md)&gt;

Creates a new proto adapter for type.
