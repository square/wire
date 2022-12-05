//[wire-schema](../../../index.md)/[com.squareup.wire.schema](../index.md)/[ClaimedDefinitions](index.md)

# ClaimedDefinitions

[common]\
class [ClaimedDefinitions](index.md)

[ClaimedDefinitions](index.md) tracks handled objects: [Type](../-type/index.md)s, [Service](../-service/index.md)s, and [Field](../-field/index.md)s. A [SchemaHandler](../-schema-handler/index.md) is to first check if an object has already been claimed; if yes, it is not to handle it. Otherwise, the [SchemaHandler](../-schema-handler/index.md) is to handle the object and [claim](claim.md) it. It is an error for a [SchemaHandler](../-schema-handler/index.md) to handle an object which has already been claimed.

## Constructors

| | |
|---|---|
| [ClaimedDefinitions](-claimed-definitions.md) | [common]<br>fun [ClaimedDefinitions](-claimed-definitions.md)() |

## Functions

| Name | Summary |
|---|---|
| [claim](claim.md) | [common]<br>fun [claim](claim.md)(member: [ProtoMember](../-proto-member/index.md))<br>Tracks that [member](claim.md) has been handled.<br>[common]<br>fun [claim](claim.md)(type: [ProtoType](../-proto-type/index.md))<br>fun [claim](claim.md)(type: [Type](../-type/index.md))<br>Tracks that [type](claim.md) has been handled.<br>[common]<br>fun [claim](claim.md)(service: [Service](../-service/index.md))<br>Tracks that [service](claim.md) has been handled. |
| [contains](contains.md) | [common]<br>operator fun [contains](contains.md)(member: [ProtoMember](../-proto-member/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if [member](contains.md) has already been handled.<br>[common]<br>operator fun [contains](contains.md)(type: [ProtoType](../-proto-type/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>operator fun [contains](contains.md)(type: [Type](../-type/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if [type](contains.md) has already been handled.<br>[common]<br>operator fun [contains](contains.md)(service: [Service](../-service/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns true if [service](contains.md) has already been handled. |
