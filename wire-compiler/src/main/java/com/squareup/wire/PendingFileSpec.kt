package com.squareup.wire

import com.squareup.wire.schema.Service
import com.squareup.wire.schema.Type

internal sealed class PendingFileSpec
internal data class PendingTypeFileSpec(val type: Type) : PendingFileSpec()
internal data class PendingServiceFileSpec(val service: Service) : PendingFileSpec()