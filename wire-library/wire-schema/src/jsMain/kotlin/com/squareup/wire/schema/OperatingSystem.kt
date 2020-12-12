package com.squareup.wire.schema

actual object OperatingSystem {
    actual val pathSeparator: Char
        get() = throw UnsupportedOperationException("Can't get the path separator when running with JavaScript runtime.")
}
