package com.squareup.wire.schema;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.wire.internal.Util;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Names a protocol buffer message, enumerated type, service or a scalar. This class models a
 * fully-qualified name using the protocol buffer package.
 */
public final class WireType {
  public static final WireType BOOL = new WireType("bool");
  public static final WireType BYTES = new WireType("bytes");
  public static final WireType DOUBLE = new WireType("double");
  public static final WireType FLOAT = new WireType("float");
  public static final WireType FIXED32 = new WireType("fixed32");
  public static final WireType FIXED64 = new WireType("fixed64");
  public static final WireType INT32 = new WireType("int32");
  public static final WireType INT64 = new WireType("int64");
  public static final WireType SFIXED32 = new WireType("sfixed32");
  public static final WireType SFIXED64 = new WireType("sfixed64");
  public static final WireType SINT32 = new WireType("sint32");
  public static final WireType SINT64 = new WireType("sint64");
  public static final WireType STRING = new WireType("string");
  public static final WireType UINT32 = new WireType("uint32");
  public static final WireType UINT64 = new WireType("uint64");

  static final WireType FILE_OPTIONS = get("google.protobuf", "FileOptions");
  static final WireType MESSAGE_OPTIONS = get("google.protobuf", "MessageOptions");
  static final WireType FIELD_OPTIONS = get("google.protobuf", "FieldOptions");
  static final WireType ENUM_OPTIONS = get("google.protobuf", "EnumOptions");
  static final WireType ENUM_VALUE_OPTIONS = get("google.protobuf", "EnumValueOptions");
  static final WireType SERVICE_OPTIONS = get("google.protobuf", "ServiceOptions");
  static final WireType METHOD_OPTIONS = get("google.protobuf", "MethodOptions");

  private static final ImmutableMap<String, WireType> SCALAR_TYPES
      = ImmutableMap.<String, WireType>builder()
      .put(BOOL.string, BOOL)
      .put(BYTES.string, BYTES)
      .put(DOUBLE.string, DOUBLE)
      .put(FLOAT.string, FLOAT)
      .put(FIXED32.string, FIXED32)
      .put(FIXED64.string, FIXED64)
      .put(INT32.string, INT32)
      .put(INT64.string, INT64)
      .put(SFIXED32.string, SFIXED32)
      .put(SFIXED64.string, SFIXED64)
      .put(SINT32.string, SINT32)
      .put(SINT64.string, SINT64)
      .put(STRING.string, STRING)
      .put(UINT32.string, UINT32)
      .put(UINT64.string, UINT64)
      .build();

  private final String protoPackage;

  /** A chain of enclosed message names, outermost is first. */
  private final ImmutableList<String> names;
  private final boolean isScalar;
  private final String string;

  private WireType(String scalarName) {
    this(null, ImmutableList.of(scalarName), true);
  }

  private WireType(String protoPackage, ImmutableList<String> names, boolean isScalar) {
    this.protoPackage = protoPackage;
    this.names = names;
    this.isScalar = isScalar;
    this.string = (protoPackage != null ? (protoPackage + '.') : "") + Joiner.on('.').join(names);
  }

  public String simpleName() {
    return names.get(names.size() - 1);
  }

  /** Returns the enclosing type, or null if this type is not nested in another type. */
  public WireType enclosingType() {
    if (names.size() == 1) return null;
    return new WireType(protoPackage, names.subList(0, names.size() - 1), isScalar);
  }

  public boolean isScalar() {
    return isScalar;
  }

  public boolean isPackableScalar() {
    return isScalar && !equals(STRING) && !equals(BYTES);
  }

  public boolean isFieldOptions() {
    return equals(FIELD_OPTIONS);
  }

  public boolean isMessageOptions() {
    return equals(MESSAGE_OPTIONS);
  }

  public static WireType get(String protoPackage, String name) {
    checkNotNull(name, "name");
    return new WireType(protoPackage, ImmutableList.of(name), false);
  }

  public static WireType getScalar(String name) {
    return SCALAR_TYPES.get(name);
  }

  public WireType nestedType(String name) {
    checkState(!isScalar);
    checkNotNull(name, "name");
    return new WireType(protoPackage, Util.concatenate(names, name), false);
  }

  @Override public boolean equals(Object o) {
    return o instanceof WireType
        && Objects.equals(((WireType) o).string, string);
  }

  @Override public int hashCode() {
    return string.hashCode();
  }

  @Override public String toString() {
    return string;
  }
}
