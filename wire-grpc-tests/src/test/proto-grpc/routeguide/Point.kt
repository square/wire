// Code generated by Wire protocol buffer compiler, do not edit.
// Source: routeguide.Point in routeguide/RouteGuideProto.proto
@file:Suppress(
  "DEPRECATION",
  "RUNTIME_ANNOTATION_NOT_SUPPORTED",
)

package routeguide

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_2
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.JvmField
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Long
import kotlin.Nothing
import kotlin.String
import kotlin.Suppress
import okio.ByteString

/**
 * Points are represented as latitude-longitude pairs in the E7 representation
 * (degrees multiplied by 10**7 and rounded to the nearest integer).
 * Latitudes should be in the range +/- 90 degrees and longitude should be in
 * the range +/- 180 degrees (inclusive).
 */
public class Point(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#INT32",
    schemaIndex = 0,
  )
  public val latitude: Int? = null,
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#INT32",
    schemaIndex = 1,
  )
  public val longitude: Int? = null,
  unknownFields: ByteString = ByteString.EMPTY,
) : Message<Point, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN,
  )
  override fun newBuilder(): Nothing = throw AssertionError("Builders are deprecated and only available in a javaInterop build; see https://square.github.io/wire/wire_compiler/#kotlin")

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is Point) return false
    if (unknownFields != other.unknownFields) return false
    if (latitude != other.latitude) return false
    if (longitude != other.longitude) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + (latitude?.hashCode() ?: 0)
      result = result * 37 + (longitude?.hashCode() ?: 0)
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (latitude != null) result += """latitude=$latitude"""
    if (longitude != null) result += """longitude=$longitude"""
    return result.joinToString(prefix = "Point{", separator = ", ", postfix = "}")
  }

  public fun copy(
    latitude: Int? = this.latitude,
    longitude: Int? = this.longitude,
    unknownFields: ByteString = this.unknownFields,
  ): Point = Point(latitude, longitude, unknownFields)

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<Point> = object : ProtoAdapter<Point>(
      FieldEncoding.LENGTH_DELIMITED, 
      Point::class, 
      "type.googleapis.com/routeguide.Point", 
      PROTO_2, 
      null, 
      "routeguide/RouteGuideProto.proto"
    ) {
      override fun encodedSize(`value`: Point): Int {
        var size = value.unknownFields.size
        size += ProtoAdapter.INT32.encodedSizeWithTag(1, value.latitude)
        size += ProtoAdapter.INT32.encodedSizeWithTag(2, value.longitude)
        return size
      }

      override fun encode(writer: ProtoWriter, `value`: Point) {
        ProtoAdapter.INT32.encodeWithTag(writer, 1, value.latitude)
        ProtoAdapter.INT32.encodeWithTag(writer, 2, value.longitude)
        writer.writeBytes(value.unknownFields)
      }

      override fun encode(writer: ReverseProtoWriter, `value`: Point) {
        writer.writeBytes(value.unknownFields)
        ProtoAdapter.INT32.encodeWithTag(writer, 2, value.longitude)
        ProtoAdapter.INT32.encodeWithTag(writer, 1, value.latitude)
      }

      override fun decode(reader: ProtoReader): Point {
        var latitude: Int? = null
        var longitude: Int? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> latitude = ProtoAdapter.INT32.decode(reader)
            2 -> longitude = ProtoAdapter.INT32.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return Point(
          latitude = latitude,
          longitude = longitude,
          unknownFields = unknownFields
        )
      }

      override fun redact(`value`: Point): Point = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
