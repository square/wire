package com.squareup.wire;

import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import com.squareup.wire.protos.alltypes.AllTypes
import okio.ByteString
import okio.buffer
import okio.source
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.io.IOException
import java.util.Collections

/** Tests meant for Java generated code to be executed among different JSON libraries. */
@RunWith(Parameterized::class)
class WireJsonJavaTest {
  @Parameterized.Parameter(0)
  internal lateinit var jsonLibrary: JsonLibrary

  @Parameterized.Parameter(1)
  internal lateinit var valueType: Class<Any>

  @Parameterized.Parameter(2)
  internal lateinit var value: Message<*, *>

  @Parameterized.Parameter(3)
  internal lateinit var json: String

  @Test fun serializeTest() {
    assertJsonEquals(json, jsonLibrary.toJson(value, valueType))
  }

  @Test fun deserializeTest() {
    val parsed = jsonLibrary.fromJson(json, valueType)
    assertThat(parsed).isEqualTo(value)
    assertThat(parsed.toString()).isEqualTo(value.toString())
    assertJsonEquals(
        jsonLibrary.toJson(parsed, valueType),
        jsonLibrary.toJson(value, valueType))
  }

  @Test fun omitsUnknownFields() {
    val builder = value.newBuilder()
    builder.addUnknownField(9000, FieldEncoding.FIXED32, 9000)
    builder.addUnknownField(9001, FieldEncoding.FIXED64, 9001L)
    builder.addUnknownField(9002, FieldEncoding.LENGTH_DELIMITED,
        ByteString.of('9'.toByte(), '0'.toByte(), '0'.toByte(), '2'.toByte()))
    builder.addUnknownField(9003, FieldEncoding.VARINT, 9003L)

    val newValue = builder.build()
    assertJsonEquals(json, jsonLibrary.toJson(newValue, valueType))
  }

  companion object {
    // Return a two-element list with a given repeated value
    private fun <T> list(x: T): List<T> = listOf(x, x)

    private fun allTypesIdentityBuilder(): AllTypes.Builder {
      return AllTypes.Builder()
          .opt_int32(0)
          .opt_uint32(0)
          .opt_sint32(0)
          .opt_fixed32(0)
          .opt_sfixed32(0)
          .opt_int64(0L)
          .opt_uint64(0L)
          .opt_sint64(0L)
          .opt_fixed64(0L)
          .opt_sfixed64(0L)
          .opt_bool(false)
          .opt_float(0f)
          .opt_double(0.0)
          .opt_string("")
          .opt_bytes(ByteString.EMPTY)
          .opt_nested_enum(AllTypes.NestedEnum.A)
          .opt_nested_message(null)
          .req_int32(0)
          .req_uint32(0)
          .req_sint32(0)
          .req_fixed32(0)
          .req_sfixed32(0)
          .req_int64(0L)
          .req_uint64(0L)
          .req_sint64(0L)
          .req_fixed64(0L)
          .req_sfixed64(0L)
          .req_bool(true)
          .req_float(0f)
          .req_double(0.0)
          .req_string("")
          .req_bytes(ByteString.EMPTY)
          .req_nested_enum(AllTypes.NestedEnum.A)
          .req_nested_message(AllTypes.NestedMessage.Builder().a(0).build())
    }

    private fun allTypesBuilder(): AllTypes.Builder {
      val bytes = ByteString.of(123.toByte(), 125.toByte())
      val nestedMessage = AllTypes.NestedMessage.Builder().a(999).build()
      return AllTypes.Builder()
          .opt_int32(111)
          .opt_uint32(112)
          .opt_sint32(113)
          .opt_fixed32(114)
          .opt_sfixed32(115)
          .opt_int64(116L)
          .opt_uint64(117L)
          .opt_sint64(118L)
          .opt_fixed64(119L)
          .opt_sfixed64(120L)
          .opt_bool(true)
          .opt_float(122.0f)
          .opt_double(123.0)
          .opt_string("124")
          .opt_bytes(bytes)
          .opt_nested_enum(AllTypes.NestedEnum.A)
          .opt_nested_message(nestedMessage)
          .req_int32(111)
          .req_uint32(112)
          .req_sint32(113)
          .req_fixed32(114)
          .req_sfixed32(115)
          .req_int64(116L)
          .req_uint64(117L)
          .req_sint64(118L)
          .req_fixed64(119L)
          .req_sfixed64(120L)
          .req_bool(true)
          .req_float(122.0f)
          .req_double(123.0)
          .req_string("124")
          .req_bytes(bytes)
          .req_nested_enum(AllTypes.NestedEnum.A)
          .req_nested_message(nestedMessage)
          .rep_int32(list(111))
          .rep_uint32(list(112))
          .rep_sint32(list(113))
          .rep_fixed32(list(114))
          .rep_sfixed32(list(115))
          .rep_int64(list(116L))
          .rep_uint64(list(117L))
          .rep_sint64(list(118L))
          .rep_fixed64(list(119L))
          .rep_sfixed64(list(120L))
          .rep_bool(list(true))
          .rep_float(list(122.0f))
          .rep_double(list(123.0))
          .rep_string(list("124"))
          .rep_bytes(list(bytes))
          .rep_nested_enum(list(AllTypes.NestedEnum.A))
          .rep_nested_message(list(nestedMessage))
          .pack_int32(list(111))
          .pack_uint32(list(112))
          .pack_sint32(list(113))
          .pack_fixed32(list(114))
          .pack_sfixed32(list(115))
          .pack_int64(list(116L))
          .pack_uint64(list(117L))
          .pack_sint64(list(118L))
          .pack_fixed64(list(119L))
          .pack_sfixed64(list(120L))
          .pack_bool(list(true))
          .pack_float(list(122.0f))
          .pack_double(list(123.0))
          .pack_nested_enum(list(AllTypes.NestedEnum.A))
          .map_int32_int32(Collections.singletonMap(1, 2))
          .map_string_string(Collections.singletonMap("key", "value"))
          .map_string_message(Collections.singletonMap("message", AllTypes.NestedMessage(1)))
          .map_string_enum(Collections.singletonMap("enum", AllTypes.NestedEnum.A))
          .oneof_int32(4444)
          .ext_opt_int32(Int.MAX_VALUE)
          .ext_opt_int64(Long.MIN_VALUE / 2 + 178)
          .ext_opt_uint64(Long.MIN_VALUE / 2 + 178)
          .ext_opt_sint64(Long.MIN_VALUE / 2 + 178)
          .ext_opt_bool(true)
          .ext_opt_float(1.2345e6f)
          .ext_opt_double(1.2345e67)
          .ext_opt_nested_enum(AllTypes.NestedEnum.A)
          .ext_opt_nested_message(nestedMessage)
          .ext_rep_int32(list(Int.MAX_VALUE))
          .ext_rep_uint64(list(Long.MIN_VALUE / 2 + 178))
          .ext_rep_sint64(list(Long.MIN_VALUE / 2 + 178))
          .ext_rep_bool(list(true))
          .ext_rep_float(list(1.2345e6f))
          .ext_rep_double(list(1.2345e67))
          .ext_rep_nested_enum(list(AllTypes.NestedEnum.A))
          .ext_rep_nested_message(list(nestedMessage))
          .ext_pack_int32(list(Int.MAX_VALUE))
          .ext_pack_uint64(list(Long.MIN_VALUE / 2 + 178))
          .ext_pack_sint64(list(Long.MIN_VALUE / 2 + 178))
          .ext_pack_bool(list(true))
          .ext_pack_float(list(1.2345e6f))
          .ext_pack_double(list(1.2345e67))
          .ext_pack_nested_enum(list(AllTypes.NestedEnum.A))
          .ext_map_int32_int32(Collections.singletonMap(1, 2))
          .ext_map_string_string(Collections.singletonMap("key", "value"))
          .ext_map_string_message(Collections.singletonMap("message", AllTypes.NestedMessage(1)))
          .ext_map_string_enum(Collections.singletonMap("enum", AllTypes.NestedEnum.A))
    }

    private val ALL_TYPES_JSON =
        File("src/commonTest/shared/json", "all_types_proto2.json")
            .source().use { it.buffer().readUtf8() }

    private val ALL_TYPES_IDENTITY_JSON =
        File("src/commonTest/shared/json", "all_types_identity_proto2.json")
            .source().use { it.buffer().readUtf8() }

    private val moshi = object : JsonLibrary {
      private val moshi = Moshi.Builder().add(WireJsonAdapterFactory()).build()

      override fun toString() = "Moshi"

      override fun fromJson(json: String, type: Class<Any>): Any {
        return moshi.adapter(type).fromJson(json)!!
      }

      override fun toJson(value: Any, type: Class<Any>): String {
        return moshi.adapter(type).toJson(value)
      }
    }

    private val gson = object : JsonLibrary {
      private val gson = GsonBuilder().registerTypeAdapterFactory(WireTypeAdapterFactory())
          .disableHtmlEscaping()
          .create()

      override fun toString() = "Gson"

      override fun fromJson(json: String, type: Class<Any>): Any {
        return gson.fromJson(json, type)
      }

      override fun toJson(value: Any, type: Class<Any>): String {
        return gson.toJson(value, type)
      }
    }

    @Parameters(name = "{index}:{0}/{1}")
    @JvmStatic
    internal fun parameters() = listOf(
        arrayOf(gson, AllTypes::class.java, allTypesBuilder().build(), ALL_TYPES_JSON),
        arrayOf(moshi, AllTypes::class.java, allTypesBuilder().build(), ALL_TYPES_JSON),
        arrayOf(gson, AllTypes::class.java, allTypesIdentityBuilder().build(), ALL_TYPES_IDENTITY_JSON),
        arrayOf(moshi, AllTypes::class.java, allTypesIdentityBuilder().build(), ALL_TYPES_IDENTITY_JSON)
    )
  }
}

internal interface JsonLibrary {
  fun fromJson(json: String, type: Class<Any>): Any
  fun toJson(value: Any, type: Class<Any>): String
}
