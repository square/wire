// Protocol Buffers - Google's data interchange format
// Copyright 2013 Google Inc.  All rights reserved.
// http://code.google.com/p/protobuf/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.squareup.wire;

/**
 * This class is used internally by the Protocol Buffer library and generated
 * message implementations.  It is public only because those generated messages
 * do not reside in the {@code protobuf} package.  Others should not use this
 * class directly.
 *
 * This class contains constants and helper functions useful for dealing with
 * the Protocol Buffer wire format.
 *
 * @author kenton@google.com Kenton Varda
 */
final class WireFormatNano {

  // Do not allow instantiation.
  private WireFormatNano() {
  }

  public static final int WIRETYPE_VARINT           = 0;
  public static final int WIRETYPE_FIXED64          = 1;
  public static final int WIRETYPE_LENGTH_DELIMITED = 2;
  public static final int WIRETYPE_START_GROUP      = 3;
  public static final int WIRETYPE_END_GROUP        = 4;
  public static final int WIRETYPE_FIXED32          = 5;

  public static final int TAG_TYPE_BITS = 3;
  public static final int TAG_TYPE_MASK = (1 << TAG_TYPE_BITS) - 1;

  /** Given a tag value, determines the wire type (the lower 3 bits). */
  static int getTagWireType(final int tag) {
    return tag & TAG_TYPE_MASK;
  }

  /** Given a tag value, determines the field number (the upper 29 bits). */
  public static int getTagFieldNumber(final int tag) {
    return tag >>> TAG_TYPE_BITS;
  }

  /** Makes a tag value given a field number and wire type. */
  public static int makeTag(final int fieldNumber, final int wireType) {
    return (fieldNumber << TAG_TYPE_BITS) | wireType;
  }
}
