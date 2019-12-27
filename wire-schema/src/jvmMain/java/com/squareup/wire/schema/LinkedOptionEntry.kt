package com.squareup.wire.schema

import com.squareup.wire.schema.internal.parser.OptionElement

data class LinkedOptionEntry(
  val optionElement: OptionElement,
  val protoMember: ProtoMember,
  val value: Any?
)
