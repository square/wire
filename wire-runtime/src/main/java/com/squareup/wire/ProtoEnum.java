/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire;

/**
 * Interface for generated {@link Enum} values to help serialization and
 * deserialization.
 *
 * The first enum value defined in any implementing class must be named {@code __UNDEFINED__}
 * and have the value {@link #UNDEFINED_VALUE}. Application code should never access this value
 * directly. Withing a parsed Message, it represents an unknown enum value which may have been
 * defined in a version of the protocol buffer definitions later than the one used to build the
 * application. An application should typically handle such a value only as part of a
 * {@code default} clause in a {@code switch} statement. For example:
 *
 * <pre>
 * switch (myMessage.someEnumValue) {
 *   case SOME_OPTION:
 *     // We understand this value.
 *     doStuff();
 *     break;
 *   case SOME_OTHER_OPTION:
 *     // We understand this value.
 *     doOtherStuff();
 *     break;
 *   default:
 *     // We don't understand this value since it was
 *     // defined after this code was written.
 *     handleUnknownValue();
 *     break;
 * }
 *
 * By including an {@code __UNDEFINED__} value in the parsed message, application code avoids having
 * to perform a separate check for {@code myMessage.someEnumValue == null} before entering the
 * switch statement.
 * </pre>
 */
public interface ProtoEnum {

  /**
   * The tag value of the {@code __UNDEFINED__} enum value.
   */
  int UNDEFINED_VALUE = Integer.MIN_VALUE;

  /**
   * The tag value of an enum constant.
   */
  int getValue();
}
