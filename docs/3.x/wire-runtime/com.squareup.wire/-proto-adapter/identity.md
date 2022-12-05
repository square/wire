//[wire-runtime](../../../index.md)/[com.squareup.wire](../index.md)/[ProtoAdapter](index.md)/[identity](identity.md)

# identity

[common]\
val [identity](identity.md): [E](index.md)?

A special value that is used when a field is absent from an encoded proto3 message. When encoding a proto3 message, fields that hold this value will be omitted.

| TYPE                                           | IDENTITY                      |\
| :--------------------------------------------- | :---------------------------- |\
| All numeric types (int32, float, double, etc.) | 0                             |\
| Boolean                                        | false                         |\
| String                                         | empty string: ""              |\
| Bytes                                          | empty bytes: ByteString.EMPTY |\
| Enums                                          | enum constant with tag 0      |\
| Lists (repeated types)                         | empty list: listOf()          |

[js, jvm, native]\
[js, jvm, native]\
val [identity](identity.md): [E](index.md)?
