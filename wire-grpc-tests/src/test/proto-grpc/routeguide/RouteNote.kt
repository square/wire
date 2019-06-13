// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: routeguide/RouteGuideProto.proto
package routeguide

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.TagHandler
import com.squareup.wire.WireField
import kotlin.AssertionError
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
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

  companion object {
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
            else -> TagHandler.UNKNOWN_TAG
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
