package com.squareup.wire.schema

import com.squareup.wire.schema.internal.ProfileFileElement

expect class Profile(
  profileFiles: List<ProfileFileElement> = emptyList()
)
