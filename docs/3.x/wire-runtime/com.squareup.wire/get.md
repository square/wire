//[wire-runtime](../../index.md)/[com.squareup.wire](index.md)/[get](get.md)

# get

[jvm]\
fun &lt;[T](get.md)&gt; [get](get.md)(value: [T](get.md)?, defaultValue: [T](get.md)): [T](get.md)

Returns value if it is not null; defaultValue otherwise. This is used to conveniently return a default value when a value is null. For example,

MyProto myProto = ...\
MyField field = Wire.get(myProto.f, MyProto.f_default);

will attempt to retrieve the value of the field 'f' defined by MyProto. If the field is null (i.e., unset), get will return its second argument, which in this case is the default value for the field 'f'.
