/*
 * Copyright (c) 2016, the R8 project authors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of Google Inc. nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
// Methods from https://r8.googlesource.com/r8/+/master/src/test/java/com/android/tools/r8/ir/desugar/backports/MathMethods.java
package com.squareup.wire.internal

internal fun addExactLong(x: Long, y: Long): Long {
  val result: Long = x + y
  if ((x xor y < 0L) or ((x xor result) >= 0L)) {
    return result
  }
  throw ArithmeticException()
}

internal fun floorDivLong(dividend: Long, divisor: Long): Long {
  val div = dividend / divisor
  val rem = dividend - divisor * div
  if (rem == 0L) {
    return div
  }
  // Normal Java division rounds towards 0. We just have to deal with the cases where rounding
  // towards 0 is wrong, which typically depends on the sign of dividend / divisor.
  //
  // signum is 1 if dividend and divisor are both nonnegative or negative, and -1 otherwise.
  val signum = 1L or ((dividend xor divisor) shr Long.SIZE_BITS - 1)
  return if (signum < 0L) div - 1L else div
}

internal fun floorModLong(dividend: Long, divisor: Long): Long {
  val rem = dividend % divisor
  if (rem == 0L) {
    return 0L
  }
  // Normal Java remainder tracks the sign of the dividend. We just have to deal with the case
  // where the resulting sign is incorrect which is when the signs do not match.
  //
  // signum is 1 if dividend and divisor are both nonnegative or negative, and -1 otherwise.
  val signum = 1L or ((dividend xor divisor) shr Long.SIZE_BITS - 1)
  return if (signum > 0L) rem else rem + divisor
}

internal const val NANOS_PER_SECOND = 1000_000_000L
