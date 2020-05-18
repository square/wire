// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: airport_location.proto
package com.squareup.wire.protos

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

class AirportLocation(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    jsonName = "airportName"
  )
  @JvmField
  val airport_name: String = "",
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    jsonName = "cityName"
  )
  @JvmField
  val city_name: String = "",
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    jsonName = "countryName"
  )
  @JvmField
  val country_name: String = "",
  unknownFields: ByteString = ByteString.EMPTY
) : Message<AirportLocation, AirportLocation.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.airport_name = airport_name
    builder.city_name = city_name
    builder.country_name = country_name
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is AirportLocation) return false
    return unknownFields == other.unknownFields
        && airport_name == other.airport_name
        && city_name == other.city_name
        && country_name == other.country_name
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + airport_name.hashCode()
      result = result * 37 + city_name.hashCode()
      result = result * 37 + country_name.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    result += """airport_name=${sanitize(airport_name)}"""
    result += """city_name=${sanitize(city_name)}"""
    result += """country_name=${sanitize(country_name)}"""
    return result.joinToString(prefix = "AirportLocation{", separator = ", ", postfix = "}")
  }

  fun copy(
    airport_name: String = this.airport_name,
    city_name: String = this.city_name,
    country_name: String = this.country_name,
    unknownFields: ByteString = this.unknownFields
  ): AirportLocation = AirportLocation(airport_name, city_name, country_name, unknownFields)

  class Builder : Message.Builder<AirportLocation, Builder>() {
    @JvmField
    var airport_name: String = ""

    @JvmField
    var city_name: String = ""

    @JvmField
    var country_name: String = ""

    fun airport_name(airport_name: String): Builder {
      this.airport_name = airport_name
      return this
    }

    fun city_name(city_name: String): Builder {
      this.city_name = city_name
      return this
    }

    fun country_name(country_name: String): Builder {
      this.country_name = country_name
      return this
    }

    override fun build(): AirportLocation = AirportLocation(
      airport_name = airport_name,
      city_name = city_name,
      country_name = country_name,
      unknownFields = buildUnknownFields()
    )
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<AirportLocation> = object : ProtoAdapter<AirportLocation>(
      FieldEncoding.LENGTH_DELIMITED, 
      AirportLocation::class, 
      "type.googleapis.com/squareup.protos.airoport.AirportLocation"
    ) {
      override fun encodedSize(value: AirportLocation): Int = 
        ProtoAdapter.STRING.encodedSizeWithTag(1, value.airport_name) +
        ProtoAdapter.STRING.encodedSizeWithTag(2, value.city_name) +
        ProtoAdapter.STRING.encodedSizeWithTag(3, value.country_name) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: AirportLocation) {
        if (value.airport_name != "") ProtoAdapter.STRING.encodeWithTag(writer, 1,
            value.airport_name)
        if (value.city_name != "") ProtoAdapter.STRING.encodeWithTag(writer, 2, value.city_name)
        if (value.country_name != "") ProtoAdapter.STRING.encodeWithTag(writer, 3,
            value.country_name)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): AirportLocation {
        var airport_name: String = ""
        var city_name: String = ""
        var country_name: String = ""
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> airport_name = ProtoAdapter.STRING.decode(reader)
            2 -> city_name = ProtoAdapter.STRING.decode(reader)
            3 -> country_name = ProtoAdapter.STRING.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return AirportLocation(
          airport_name = airport_name,
          city_name = city_name,
          country_name = country_name,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: AirportLocation): AirportLocation = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
