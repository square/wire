//[wire-grpc-server](../../../index.md)/[com.squareup.wire.kotlin.grpcserver](../index.md)/[MessageSourceAdapter](index.md)

# MessageSourceAdapter

[jvm]\
class [MessageSourceAdapter](index.md)&lt;[T](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; : MessageSource&lt;[T](index.md)&gt; , StreamObserver&lt;[T](index.md)&gt;

## Functions

| Name | Summary |
|---|---|
| [close](close.md) | [jvm]<br>open override fun [close](close.md)() |
| [onCompleted](on-completed.md) | [jvm]<br>open override fun [onCompleted](on-completed.md)() |
| [onError](on-error.md) | [jvm]<br>open override fun [onError](on-error.md)(t: [Throwable](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)) |
| [onNext](on-next.md) | [jvm]<br>open override fun [onNext](on-next.md)(next: [T](index.md)) |
| [read](read.md) | [jvm]<br>open override fun [read](read.md)(): [T](index.md)? |
