package com.squareup.wire.descriptor.sample

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type
import com.squareup.wire.point.Point
import com.squareup.wire.protos.ProtoTypeDescriptor
import com.squareup.wire.whiteboard.WhiteBoardEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProtoTypeDescriptorIntegrationTest {
  @Test
  fun getMessageDescriptor() {
    val event = WhiteBoardEvent.BoardUpdated(
      points = listOf(
        Point(3, 3, Point.Color.RED),
        Point(0, 0, Point.Color.YELLOW)
      )
    )

    val descriptor = ProtoTypeDescriptor.getMessageDescriptor(event)!!
    assertThat(descriptor.toProto()).isEqualTo(
      DescriptorProtos.DescriptorProto.newBuilder()
        .setName("BoardUpdated")
        .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
          .setName("points")
          .setNumber(1)
          .setLabel(Label.LABEL_REPEATED)
          .setType(Type.TYPE_MESSAGE)
          .setTypeName(".${Point::class.java.canonicalName}")
          .build())
        .build()
    )
  }
}