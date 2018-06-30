import com.squareup.wire.FieldEncoding
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.internal.Internal
import com.squareup.wire.kotlin.UnkownFieldsBuilder
import com.squareup.wire.kotlin.decodeMessage
import kotlin.Float
import kotlin.Int
import okio.ByteString

data class Message(val Float: Float, val unknownFields: ByteString = ByteString.EMPTY) {
  object ADAPTER : ProtoAdapter<Message>(FieldEncoding.LENGTH_DELIMITED, Message::class.java) {
    override fun encodedSize(value: Message): Int = ProtoAdapter.FLOAT.encodedSizeWithTag(1, value.Float) +
        value.unknownFields.size()

    override fun encode(writer: ProtoWriter, value: Message) {
      ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.Float)
      writer.writeBytes(value.unknownFields)
    }

    override fun decode(reader: ProtoReader): Message {
      var Float: Float? = null
      val unknownFields = reader.decodeMessage { tag ->
        when (tag) {
          1 -> Float = ProtoAdapter.FLOAT.decode(reader)
          else -> UnkownFieldsBuilder.UNKNOWN_FIELD
        }
      }
      return Message(
          Float = Float ?: throw Internal.missingRequiredFields(Float, "Float"),
          unknownFields = unknownFields
      )
    }
  }
}