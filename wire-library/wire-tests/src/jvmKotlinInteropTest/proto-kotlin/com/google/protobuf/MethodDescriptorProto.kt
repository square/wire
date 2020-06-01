// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto
package com.google.protobuf

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.sanitize
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

/**
 * Describes a method of a service.
 */
class MethodDescriptorProto(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  @JvmField
  val name: String? = null,
  /**
   * Input and output type names.  These are resolved in the same way as
   * FieldDescriptorProto.type_name, but must refer to a message type.
   */
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  @JvmField
  val input_type: String? = null,
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  @JvmField
  val output_type: String? = null,
  @field:WireField(
    tag = 4,
    adapter = "com.google.protobuf.MethodOptions#ADAPTER"
  )
  @JvmField
  val options: MethodOptions? = null,
  /**
   * Identifies if client streams multiple client messages
   */
  @field:WireField(
    tag = 5,
    adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  @JvmField
  val client_streaming: Boolean? = null,
  /**
   * Identifies if server streams multiple server messages
   */
  @field:WireField(
    tag = 6,
    adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  @JvmField
  val server_streaming: Boolean? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<MethodDescriptorProto, MethodDescriptorProto.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.name = name
    builder.input_type = input_type
    builder.output_type = output_type
    builder.options = options
    builder.client_streaming = client_streaming
    builder.server_streaming = server_streaming
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is MethodDescriptorProto) return false
    if (unknownFields != other.unknownFields) return false
    if (name != other.name) return false
    if (input_type != other.input_type) return false
    if (output_type != other.output_type) return false
    if (options != other.options) return false
    if (client_streaming != other.client_streaming) return false
    if (server_streaming != other.server_streaming) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + name.hashCode()
      result = result * 37 + input_type.hashCode()
      result = result * 37 + output_type.hashCode()
      result = result * 37 + options.hashCode()
      result = result * 37 + client_streaming.hashCode()
      result = result * 37 + server_streaming.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (name != null) result += """name=${sanitize(name)}"""
    if (input_type != null) result += """input_type=${sanitize(input_type)}"""
    if (output_type != null) result += """output_type=${sanitize(output_type)}"""
    if (options != null) result += """options=$options"""
    if (client_streaming != null) result += """client_streaming=$client_streaming"""
    if (server_streaming != null) result += """server_streaming=$server_streaming"""
    return result.joinToString(prefix = "MethodDescriptorProto{", separator = ", ", postfix = "}")
  }

  fun copy(
    name: String? = this.name,
    input_type: String? = this.input_type,
    output_type: String? = this.output_type,
    options: MethodOptions? = this.options,
    client_streaming: Boolean? = this.client_streaming,
    server_streaming: Boolean? = this.server_streaming,
    unknownFields: ByteString = this.unknownFields
  ): MethodDescriptorProto = MethodDescriptorProto(name, input_type, output_type, options,
      client_streaming, server_streaming, unknownFields)

  class Builder : Message.Builder<MethodDescriptorProto, Builder>() {
    @JvmField
    var name: String? = null

    @JvmField
    var input_type: String? = null

    @JvmField
    var output_type: String? = null

    @JvmField
    var options: MethodOptions? = null

    @JvmField
    var client_streaming: Boolean? = null

    @JvmField
    var server_streaming: Boolean? = null

    fun name(name: String?): Builder {
      this.name = name
      return this
    }

    /**
     * Input and output type names.  These are resolved in the same way as
     * FieldDescriptorProto.type_name, but must refer to a message type.
     */
    fun input_type(input_type: String?): Builder {
      this.input_type = input_type
      return this
    }

    fun output_type(output_type: String?): Builder {
      this.output_type = output_type
      return this
    }

    fun options(options: MethodOptions?): Builder {
      this.options = options
      return this
    }

    /**
     * Identifies if client streams multiple client messages
     */
    fun client_streaming(client_streaming: Boolean?): Builder {
      this.client_streaming = client_streaming
      return this
    }

    /**
     * Identifies if server streams multiple server messages
     */
    fun server_streaming(server_streaming: Boolean?): Builder {
      this.server_streaming = server_streaming
      return this
    }

    override fun build(): MethodDescriptorProto = MethodDescriptorProto(
      name = name,
      input_type = input_type,
      output_type = output_type,
      options = options,
      client_streaming = client_streaming,
      server_streaming = server_streaming,
      unknownFields = buildUnknownFields()
    )
  }

  companion object {
    const val DEFAULT_CLIENT_STREAMING: Boolean = false

    const val DEFAULT_SERVER_STREAMING: Boolean = false

    @JvmField
    val ADAPTER: ProtoAdapter<MethodDescriptorProto> = object : ProtoAdapter<MethodDescriptorProto>(
      FieldEncoding.LENGTH_DELIMITED, 
      MethodDescriptorProto::class, 
      "type.googleapis.com/google.protobuf.MethodDescriptorProto"
    ) {
      override fun encodedSize(value: MethodDescriptorProto): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
        size += ProtoAdapter.STRING.encodedSizeWithTag(2, value.input_type)
        size += ProtoAdapter.STRING.encodedSizeWithTag(3, value.output_type)
        size += MethodOptions.ADAPTER.encodedSizeWithTag(4, value.options)
        size += ProtoAdapter.BOOL.encodedSizeWithTag(5, value.client_streaming)
        size += ProtoAdapter.BOOL.encodedSizeWithTag(6, value.server_streaming)
        return size
      }

      override fun encode(writer: ProtoWriter, value: MethodDescriptorProto) {
        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
        ProtoAdapter.STRING.encodeWithTag(writer, 2, value.input_type)
        ProtoAdapter.STRING.encodeWithTag(writer, 3, value.output_type)
        MethodOptions.ADAPTER.encodeWithTag(writer, 4, value.options)
        ProtoAdapter.BOOL.encodeWithTag(writer, 5, value.client_streaming)
        ProtoAdapter.BOOL.encodeWithTag(writer, 6, value.server_streaming)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): MethodDescriptorProto {
        var name: String? = null
        var input_type: String? = null
        var output_type: String? = null
        var options: MethodOptions? = null
        var client_streaming: Boolean? = null
        var server_streaming: Boolean? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> name = ProtoAdapter.STRING.decode(reader)
            2 -> input_type = ProtoAdapter.STRING.decode(reader)
            3 -> output_type = ProtoAdapter.STRING.decode(reader)
            4 -> options = MethodOptions.ADAPTER.decode(reader)
            5 -> client_streaming = ProtoAdapter.BOOL.decode(reader)
            6 -> server_streaming = ProtoAdapter.BOOL.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return MethodDescriptorProto(
          name = name,
          input_type = input_type,
          output_type = output_type,
          options = options,
          client_streaming = client_streaming,
          server_streaming = server_streaming,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: MethodDescriptorProto): MethodDescriptorProto = value.copy(
        options = value.options?.let(MethodOptions.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
