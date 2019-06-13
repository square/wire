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
import okio.ByteString

/**
 * A RouteSummary is received in response to a RecordRoute rpc.
 *
 * It contains the number of individual points received, the number of
 * detected features, and the total distance covered as the cumulative sum of
 * the distance between each point.
 */
data class RouteSummary(
  /**
   * The number of points received.
   */
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  val point_count: Int? = null,
  /**
   * The number of known features passed while traversing the route.
   */
  @field:WireField(
    tag = 2,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  val feature_count: Int? = null,
  /**
   * The distance covered in metres.
   */
  @field:WireField(
    tag = 3,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  val distance: Int? = null,
  /**
   * The duration of the traversal in seconds.
   */
  @field:WireField(
    tag = 4,
    adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  val elapsed_time: Int? = null,
  val unknownFields: ByteString = ByteString.EMPTY
) : Message<RouteSummary, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing {
    throw AssertionError()
  }

  companion object {
    val ADAPTER: ProtoAdapter<RouteSummary> = object : ProtoAdapter<RouteSummary>(
      FieldEncoding.LENGTH_DELIMITED, 
      RouteSummary::class
    ) {
      override fun encodedSize(value: RouteSummary): Int = 
        ProtoAdapter.INT32.encodedSizeWithTag(1, value.point_count) +
        ProtoAdapter.INT32.encodedSizeWithTag(2, value.feature_count) +
        ProtoAdapter.INT32.encodedSizeWithTag(3, value.distance) +
        ProtoAdapter.INT32.encodedSizeWithTag(4, value.elapsed_time) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: RouteSummary) {
        ProtoAdapter.INT32.encodeWithTag(writer, 1, value.point_count)
        ProtoAdapter.INT32.encodeWithTag(writer, 2, value.feature_count)
        ProtoAdapter.INT32.encodeWithTag(writer, 3, value.distance)
        ProtoAdapter.INT32.encodeWithTag(writer, 4, value.elapsed_time)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): RouteSummary {
        var point_count: Int? = null
        var feature_count: Int? = null
        var distance: Int? = null
        var elapsed_time: Int? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> point_count = ProtoAdapter.INT32.decode(reader)
            2 -> feature_count = ProtoAdapter.INT32.decode(reader)
            3 -> distance = ProtoAdapter.INT32.decode(reader)
            4 -> elapsed_time = ProtoAdapter.INT32.decode(reader)
            else -> TagHandler.UNKNOWN_TAG
          }
        }
        return RouteSummary(
          point_count = point_count,
          feature_count = feature_count,
          distance = distance,
          elapsed_time = elapsed_time,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: RouteSummary): RouteSummary = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
