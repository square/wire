// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.proto3.All64 in all64.proto
package squareup.proto3;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.Syntax;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import okio.ByteString;

public final class All64 extends Message<All64, All64.Builder> {
  public static final ProtoAdapter<All64> ADAPTER = ProtoAdapter.newMessageAdapter(All64.class, "type.googleapis.com/squareup.proto3.All64", Syntax.PROTO_3);

  private static final long serialVersionUID = 0L;

  public static final Long DEFAULT_ONEOF_INT64 = 0L;

  public static final Long DEFAULT_ONEOF_SFIXED64 = 0L;

  /**
   * Prefixing so the generated code doesn't rename it weirdly.
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#INT64",
      label = WireField.Label.OMIT_IDENTITY,
      jsonName = "myInt64"
  )
  public final long my_int64;

  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#UINT64",
      label = WireField.Label.OMIT_IDENTITY,
      jsonName = "myUint64"
  )
  public final long my_uint64;

  @WireField(
      tag = 3,
      adapter = "com.squareup.wire.ProtoAdapter#SINT64",
      label = WireField.Label.OMIT_IDENTITY,
      jsonName = "mySint64"
  )
  public final long my_sint64;

  @WireField(
      tag = 4,
      adapter = "com.squareup.wire.ProtoAdapter#FIXED64",
      label = WireField.Label.OMIT_IDENTITY,
      jsonName = "myFixed64"
  )
  public final long my_fixed64;

  @WireField(
      tag = 5,
      adapter = "com.squareup.wire.ProtoAdapter#SFIXED64",
      label = WireField.Label.OMIT_IDENTITY,
      jsonName = "mySfixed64"
  )
  public final long my_sfixed64;

  @WireField(
      tag = 201,
      adapter = "com.squareup.wire.ProtoAdapter#INT64",
      label = WireField.Label.REPEATED,
      jsonName = "repInt64"
  )
  public final List<Long> rep_int64;

  @WireField(
      tag = 202,
      adapter = "com.squareup.wire.ProtoAdapter#UINT64",
      label = WireField.Label.REPEATED,
      jsonName = "repUint64"
  )
  public final List<Long> rep_uint64;

  @WireField(
      tag = 203,
      adapter = "com.squareup.wire.ProtoAdapter#SINT64",
      label = WireField.Label.REPEATED,
      jsonName = "repSint64"
  )
  public final List<Long> rep_sint64;

  @WireField(
      tag = 204,
      adapter = "com.squareup.wire.ProtoAdapter#FIXED64",
      label = WireField.Label.REPEATED,
      jsonName = "repFixed64"
  )
  public final List<Long> rep_fixed64;

  @WireField(
      tag = 205,
      adapter = "com.squareup.wire.ProtoAdapter#SFIXED64",
      label = WireField.Label.REPEATED,
      jsonName = "repSfixed64"
  )
  public final List<Long> rep_sfixed64;

  @WireField(
      tag = 301,
      adapter = "com.squareup.wire.ProtoAdapter#INT64",
      label = WireField.Label.PACKED,
      jsonName = "packInt64"
  )
  public final List<Long> pack_int64;

  @WireField(
      tag = 302,
      adapter = "com.squareup.wire.ProtoAdapter#UINT64",
      label = WireField.Label.PACKED,
      jsonName = "packUint64"
  )
  public final List<Long> pack_uint64;

  @WireField(
      tag = 303,
      adapter = "com.squareup.wire.ProtoAdapter#SINT64",
      label = WireField.Label.PACKED,
      jsonName = "packSint64"
  )
  public final List<Long> pack_sint64;

  @WireField(
      tag = 304,
      adapter = "com.squareup.wire.ProtoAdapter#FIXED64",
      label = WireField.Label.PACKED,
      jsonName = "packFixed64"
  )
  public final List<Long> pack_fixed64;

  @WireField(
      tag = 305,
      adapter = "com.squareup.wire.ProtoAdapter#SFIXED64",
      label = WireField.Label.PACKED,
      jsonName = "packSfixed64"
  )
  public final List<Long> pack_sfixed64;

  @WireField(
      tag = 401,
      adapter = "com.squareup.wire.ProtoAdapter#INT64",
      jsonName = "oneofInt64"
  )
  public final Long oneof_int64;

  @WireField(
      tag = 402,
      adapter = "com.squareup.wire.ProtoAdapter#SFIXED64",
      jsonName = "oneofSfixed64"
  )
  public final Long oneof_sfixed64;

  public All64(long my_int64, long my_uint64, long my_sint64, long my_fixed64, long my_sfixed64,
      List<Long> rep_int64, List<Long> rep_uint64, List<Long> rep_sint64, List<Long> rep_fixed64,
      List<Long> rep_sfixed64, List<Long> pack_int64, List<Long> pack_uint64,
      List<Long> pack_sint64, List<Long> pack_fixed64, List<Long> pack_sfixed64, Long oneof_int64,
      Long oneof_sfixed64) {
    this(my_int64, my_uint64, my_sint64, my_fixed64, my_sfixed64, rep_int64, rep_uint64, rep_sint64, rep_fixed64, rep_sfixed64, pack_int64, pack_uint64, pack_sint64, pack_fixed64, pack_sfixed64, oneof_int64, oneof_sfixed64, ByteString.EMPTY);
  }

  public All64(long my_int64, long my_uint64, long my_sint64, long my_fixed64, long my_sfixed64,
      List<Long> rep_int64, List<Long> rep_uint64, List<Long> rep_sint64, List<Long> rep_fixed64,
      List<Long> rep_sfixed64, List<Long> pack_int64, List<Long> pack_uint64,
      List<Long> pack_sint64, List<Long> pack_fixed64, List<Long> pack_sfixed64, Long oneof_int64,
      Long oneof_sfixed64, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    if (Internal.countNonNull(oneof_int64, oneof_sfixed64) > 1) {
      throw new IllegalArgumentException("at most one of oneof_int64, oneof_sfixed64 may be non-null");
    }
    this.my_int64 = my_int64;
    this.my_uint64 = my_uint64;
    this.my_sint64 = my_sint64;
    this.my_fixed64 = my_fixed64;
    this.my_sfixed64 = my_sfixed64;
    this.rep_int64 = Internal.immutableCopyOf("rep_int64", rep_int64);
    this.rep_uint64 = Internal.immutableCopyOf("rep_uint64", rep_uint64);
    this.rep_sint64 = Internal.immutableCopyOf("rep_sint64", rep_sint64);
    this.rep_fixed64 = Internal.immutableCopyOf("rep_fixed64", rep_fixed64);
    this.rep_sfixed64 = Internal.immutableCopyOf("rep_sfixed64", rep_sfixed64);
    this.pack_int64 = Internal.immutableCopyOf("pack_int64", pack_int64);
    this.pack_uint64 = Internal.immutableCopyOf("pack_uint64", pack_uint64);
    this.pack_sint64 = Internal.immutableCopyOf("pack_sint64", pack_sint64);
    this.pack_fixed64 = Internal.immutableCopyOf("pack_fixed64", pack_fixed64);
    this.pack_sfixed64 = Internal.immutableCopyOf("pack_sfixed64", pack_sfixed64);
    this.oneof_int64 = oneof_int64;
    this.oneof_sfixed64 = oneof_sfixed64;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.my_int64 = my_int64;
    builder.my_uint64 = my_uint64;
    builder.my_sint64 = my_sint64;
    builder.my_fixed64 = my_fixed64;
    builder.my_sfixed64 = my_sfixed64;
    builder.rep_int64 = Internal.copyOf(rep_int64);
    builder.rep_uint64 = Internal.copyOf(rep_uint64);
    builder.rep_sint64 = Internal.copyOf(rep_sint64);
    builder.rep_fixed64 = Internal.copyOf(rep_fixed64);
    builder.rep_sfixed64 = Internal.copyOf(rep_sfixed64);
    builder.pack_int64 = Internal.copyOf(pack_int64);
    builder.pack_uint64 = Internal.copyOf(pack_uint64);
    builder.pack_sint64 = Internal.copyOf(pack_sint64);
    builder.pack_fixed64 = Internal.copyOf(pack_fixed64);
    builder.pack_sfixed64 = Internal.copyOf(pack_sfixed64);
    builder.oneof_int64 = oneof_int64;
    builder.oneof_sfixed64 = oneof_sfixed64;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof All64)) return false;
    All64 o = (All64) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(my_int64, o.my_int64)
        && Internal.equals(my_uint64, o.my_uint64)
        && Internal.equals(my_sint64, o.my_sint64)
        && Internal.equals(my_fixed64, o.my_fixed64)
        && Internal.equals(my_sfixed64, o.my_sfixed64)
        && rep_int64.equals(o.rep_int64)
        && rep_uint64.equals(o.rep_uint64)
        && rep_sint64.equals(o.rep_sint64)
        && rep_fixed64.equals(o.rep_fixed64)
        && rep_sfixed64.equals(o.rep_sfixed64)
        && pack_int64.equals(o.pack_int64)
        && pack_uint64.equals(o.pack_uint64)
        && pack_sint64.equals(o.pack_sint64)
        && pack_fixed64.equals(o.pack_fixed64)
        && pack_sfixed64.equals(o.pack_sfixed64)
        && Internal.equals(oneof_int64, o.oneof_int64)
        && Internal.equals(oneof_sfixed64, o.oneof_sfixed64);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + Long.hashCode(my_int64);
      result = result * 37 + Long.hashCode(my_uint64);
      result = result * 37 + Long.hashCode(my_sint64);
      result = result * 37 + Long.hashCode(my_fixed64);
      result = result * 37 + Long.hashCode(my_sfixed64);
      result = result * 37 + rep_int64.hashCode();
      result = result * 37 + rep_uint64.hashCode();
      result = result * 37 + rep_sint64.hashCode();
      result = result * 37 + rep_fixed64.hashCode();
      result = result * 37 + rep_sfixed64.hashCode();
      result = result * 37 + pack_int64.hashCode();
      result = result * 37 + pack_uint64.hashCode();
      result = result * 37 + pack_sint64.hashCode();
      result = result * 37 + pack_fixed64.hashCode();
      result = result * 37 + pack_sfixed64.hashCode();
      result = result * 37 + (oneof_int64 != null ? oneof_int64.hashCode() : 0);
      result = result * 37 + (oneof_sfixed64 != null ? oneof_sfixed64.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<All64, Builder> {
    public long my_int64;

    public long my_uint64;

    public long my_sint64;

    public long my_fixed64;

    public long my_sfixed64;

    public List<Long> rep_int64;

    public List<Long> rep_uint64;

    public List<Long> rep_sint64;

    public List<Long> rep_fixed64;

    public List<Long> rep_sfixed64;

    public List<Long> pack_int64;

    public List<Long> pack_uint64;

    public List<Long> pack_sint64;

    public List<Long> pack_fixed64;

    public List<Long> pack_sfixed64;

    public Long oneof_int64;

    public Long oneof_sfixed64;

    public Builder() {
      rep_int64 = Internal.newMutableList();
      rep_uint64 = Internal.newMutableList();
      rep_sint64 = Internal.newMutableList();
      rep_fixed64 = Internal.newMutableList();
      rep_sfixed64 = Internal.newMutableList();
      pack_int64 = Internal.newMutableList();
      pack_uint64 = Internal.newMutableList();
      pack_sint64 = Internal.newMutableList();
      pack_fixed64 = Internal.newMutableList();
      pack_sfixed64 = Internal.newMutableList();
    }

    /**
     * Prefixing so the generated code doesn't rename it weirdly.
     */
    public Builder my_int64(long my_int64) {
      this.my_int64 = my_int64;
      return this;
    }

    public Builder my_uint64(long my_uint64) {
      this.my_uint64 = my_uint64;
      return this;
    }

    public Builder my_sint64(long my_sint64) {
      this.my_sint64 = my_sint64;
      return this;
    }

    public Builder my_fixed64(long my_fixed64) {
      this.my_fixed64 = my_fixed64;
      return this;
    }

    public Builder my_sfixed64(long my_sfixed64) {
      this.my_sfixed64 = my_sfixed64;
      return this;
    }

    public Builder rep_int64(List<Long> rep_int64) {
      Internal.checkElementsNotNull(rep_int64);
      this.rep_int64 = rep_int64;
      return this;
    }

    public Builder rep_uint64(List<Long> rep_uint64) {
      Internal.checkElementsNotNull(rep_uint64);
      this.rep_uint64 = rep_uint64;
      return this;
    }

    public Builder rep_sint64(List<Long> rep_sint64) {
      Internal.checkElementsNotNull(rep_sint64);
      this.rep_sint64 = rep_sint64;
      return this;
    }

    public Builder rep_fixed64(List<Long> rep_fixed64) {
      Internal.checkElementsNotNull(rep_fixed64);
      this.rep_fixed64 = rep_fixed64;
      return this;
    }

    public Builder rep_sfixed64(List<Long> rep_sfixed64) {
      Internal.checkElementsNotNull(rep_sfixed64);
      this.rep_sfixed64 = rep_sfixed64;
      return this;
    }

    public Builder pack_int64(List<Long> pack_int64) {
      Internal.checkElementsNotNull(pack_int64);
      this.pack_int64 = pack_int64;
      return this;
    }

    public Builder pack_uint64(List<Long> pack_uint64) {
      Internal.checkElementsNotNull(pack_uint64);
      this.pack_uint64 = pack_uint64;
      return this;
    }

    public Builder pack_sint64(List<Long> pack_sint64) {
      Internal.checkElementsNotNull(pack_sint64);
      this.pack_sint64 = pack_sint64;
      return this;
    }

    public Builder pack_fixed64(List<Long> pack_fixed64) {
      Internal.checkElementsNotNull(pack_fixed64);
      this.pack_fixed64 = pack_fixed64;
      return this;
    }

    public Builder pack_sfixed64(List<Long> pack_sfixed64) {
      Internal.checkElementsNotNull(pack_sfixed64);
      this.pack_sfixed64 = pack_sfixed64;
      return this;
    }

    public Builder oneof_int64(Long oneof_int64) {
      this.oneof_int64 = oneof_int64;
      this.oneof_sfixed64 = null;
      return this;
    }

    public Builder oneof_sfixed64(Long oneof_sfixed64) {
      this.oneof_sfixed64 = oneof_sfixed64;
      this.oneof_int64 = null;
      return this;
    }

    @Override
    public All64 build() {
      return new All64(my_int64, my_uint64, my_sint64, my_fixed64, my_sfixed64, rep_int64, rep_uint64, rep_sint64, rep_fixed64, rep_sfixed64, pack_int64, pack_uint64, pack_sint64, pack_fixed64, pack_sfixed64, oneof_int64, oneof_sfixed64, super.buildUnknownFields());
    }
  }
}
