// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: routeguide/RouteGuideProto.proto
package routeguide

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.jvm.JvmField
import okio.ByteString

/**
 * A RouteNote is a message sent while at a given point.
 */
data class RouteNote(
  /**
   * The location from which the message is sent.
   */
  @field:WireField(
    tag = 1,
    adapter = "routeguide.Point#ADAPTER"
  )
  val location: Point? = null,
  /**
   * The message to be sent.
   */
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  val message: String? = null,
  val unknownFields: ByteString = ByteString.EMPTY
) : Message<RouteNote, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing {
    throw AssertionError()
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is RouteNote) return false
    return unknownFields == other.unknownFields
        && location == other.location
        && message == other.message
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = result * 37 + (location?.hashCode() ?: 0)
      result = result * 37 + (message?.hashCode() ?: 0)
      super.hashCode = result
    }
    return result
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<RouteNote> = object : ProtoAdapter<RouteNote>(
      FieldEncoding.LENGTH_DELIMITED, 
      RouteNote::class
    ) {
      override fun encodedSize(value: RouteNote): Int = 
        Point.ADAPTER.encodedSizeWithTag(1, value.location) +
        ProtoAdapter.STRING.encodedSizeWithTag(2, value.message) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: RouteNote) {
        Point.ADAPTER.encodeWithTag(writer, 1, value.location)
        ProtoAdapter.STRING.encodeWithTag(writer, 2, value.message)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): RouteNote {
        var location: Point? = null
        var message: String? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> location = Point.ADAPTER.decode(reader)
            2 -> message = ProtoAdapter.STRING.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return RouteNote(
          location = location,
          message = message,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: RouteNote): RouteNote = value.copy(
        location = value.location?.let(Point.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
