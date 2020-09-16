Wire JSON
==============

Wire offers JSON serialization over [Moshi][moshi] or [Gson][gson]. We provide an adapter factory
for both to configure them to deal with protobuf specifics.

### Moshi

Moshi support is part of the `wire-moshi-adapter` module. It has to be added to the project
dependencies in order to plug the `WireJsonAdapterFactory` into Moshi.

The Maven coordinates are: `com.squareup.wire:wire-moshi-adapter:<version>`

```kotlin
val moshi = Moshi.Builder()
      .add(WireJsonAdapterFactory())
      .build()

val adapter = moshi.adapter(Pizza::class.java)
val pizza: Pizza = ...
val json = adapter.toJson(pizza)
val parsedPizza = adapter.fromJson(json)
```

### Gson

Moshi support is part of the `wire-gson-support` module. It has to be added to the project
dependencies in order to plug the `WireTypeAdapterFactory` into Gson.

The Maven coordinates are: `com.squareup.wire:wire-gson-support:<version>`

```kotlin
val gson = GsonBuilder()
      .registerTypeAdapterFactory(WireTypeAdapterFactory())
      .create()

val adapter = gson.adapter(Pizza::class.java)
val pizza: Pizza = ...
val json = adapter.toJson(pizza)
val parsedPizza = adapter.fromJson(json)
```

### Protoc

Wire is interoperable with protoc for all proto3 messages. The JSON representation of proto2 is
unspecified. JSON representations of proto2 messages from Wire and protoc are not interoperable.
Note that by default Protoc throws an error for unknown fields. Wire will ignore them.

 [gson]: https://github.com/google/gson/
 [moshi]: https://github.com/square/moshi/
