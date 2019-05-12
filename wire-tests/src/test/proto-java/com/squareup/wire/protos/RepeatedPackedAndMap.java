// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: collection_types.proto
package com.squareup.wire.protos;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.List;
import java.util.Map;
import kotlin.jvm.JvmClassMappingKt;
import okio.ByteString;

public final class RepeatedPackedAndMap extends Message<RepeatedPackedAndMap, RepeatedPackedAndMap.Builder> {
  public static final ProtoAdapter<RepeatedPackedAndMap> ADAPTER = new ProtoAdapter_RepeatedPackedAndMap();

  private static final long serialVersionUID = 0L;

  @WireField(
      tag = 201,
      adapter = "com.squareup.wire.ProtoAdapter#INT32",
      label = WireField.Label.REPEATED
  )
  public final List<Integer> rep_int32;

  @WireField(
      tag = 301,
      adapter = "com.squareup.wire.ProtoAdapter#INT32",
      label = WireField.Label.PACKED
  )
  public final List<Integer> pack_int32;

  @WireField(
      tag = 401,
      keyAdapter = "com.squareup.wire.ProtoAdapter#INT32",
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Map<Integer, Integer> map_int32_int32;

  public RepeatedPackedAndMap(List<Integer> rep_int32, List<Integer> pack_int32,
      Map<Integer, Integer> map_int32_int32) {
    this(rep_int32, pack_int32, map_int32_int32, ByteString.EMPTY);
  }

  public RepeatedPackedAndMap(List<Integer> rep_int32, List<Integer> pack_int32,
      Map<Integer, Integer> map_int32_int32, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.rep_int32 = Internal.immutableCopyOf("rep_int32", rep_int32);
    this.pack_int32 = Internal.immutableCopyOf("pack_int32", pack_int32);
    this.map_int32_int32 = Internal.immutableCopyOf("map_int32_int32", map_int32_int32);
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.rep_int32 = Internal.copyOf("rep_int32", rep_int32);
    builder.pack_int32 = Internal.copyOf("pack_int32", pack_int32);
    builder.map_int32_int32 = Internal.copyOf("map_int32_int32", map_int32_int32);
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof RepeatedPackedAndMap)) return false;
    RepeatedPackedAndMap o = (RepeatedPackedAndMap) other;
    return unknownFields().equals(o.unknownFields())
        && rep_int32.equals(o.rep_int32)
        && pack_int32.equals(o.pack_int32)
        && map_int32_int32.equals(o.map_int32_int32);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + rep_int32.hashCode();
      result = result * 37 + pack_int32.hashCode();
      result = result * 37 + map_int32_int32.hashCode();
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (!rep_int32.isEmpty()) builder.append(", rep_int32=").append(rep_int32);
    if (!pack_int32.isEmpty()) builder.append(", pack_int32=").append(pack_int32);
    if (!map_int32_int32.isEmpty()) builder.append(", map_int32_int32=").append(map_int32_int32);
    return builder.replace(0, 2, "RepeatedPackedAndMap{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<RepeatedPackedAndMap, Builder> {
    public List<Integer> rep_int32;

    public List<Integer> pack_int32;

    public Map<Integer, Integer> map_int32_int32;

    public Builder() {
      rep_int32 = Internal.newMutableList();
      pack_int32 = Internal.newMutableList();
      map_int32_int32 = Internal.newMutableMap();
    }

    public Builder rep_int32(List<Integer> rep_int32) {
      Internal.checkElementsNotNull(rep_int32);
      this.rep_int32 = rep_int32;
      return this;
    }

    public Builder pack_int32(List<Integer> pack_int32) {
      Internal.checkElementsNotNull(pack_int32);
      this.pack_int32 = pack_int32;
      return this;
    }

    public Builder map_int32_int32(Map<Integer, Integer> map_int32_int32) {
      Internal.checkElementsNotNull(map_int32_int32);
      this.map_int32_int32 = map_int32_int32;
      return this;
    }

    @Override
    public RepeatedPackedAndMap build() {
      return new RepeatedPackedAndMap(rep_int32, pack_int32, map_int32_int32, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_RepeatedPackedAndMap extends ProtoAdapter<RepeatedPackedAndMap> {
    private final ProtoAdapter<Map<Integer, Integer>> map_int32_int32 = ProtoAdapter.newMapAdapter(ProtoAdapter.INT32, ProtoAdapter.INT32);

    public ProtoAdapter_RepeatedPackedAndMap() {
      super(FieldEncoding.LENGTH_DELIMITED, JvmClassMappingKt.getKotlinClass(RepeatedPackedAndMap.class));
    }

    @Override
    public int encodedSize(RepeatedPackedAndMap value) {
      return ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(201, value.rep_int32)
          + ProtoAdapter.INT32.asPacked().encodedSizeWithTag(301, value.pack_int32)
          + map_int32_int32.encodedSizeWithTag(401, value.map_int32_int32)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, RepeatedPackedAndMap value) throws IOException {
      ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 201, value.rep_int32);
      ProtoAdapter.INT32.asPacked().encodeWithTag(writer, 301, value.pack_int32);
      map_int32_int32.encodeWithTag(writer, 401, value.map_int32_int32);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public RepeatedPackedAndMap decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 201: builder.rep_int32.add(ProtoAdapter.INT32.decode(reader)); break;
          case 301: builder.pack_int32.add(ProtoAdapter.INT32.decode(reader)); break;
          case 401: builder.map_int32_int32.putAll(map_int32_int32.decode(reader)); break;
          default: {
            FieldEncoding fieldEncoding = reader.peekFieldEncoding();
            Object value = fieldEncoding.rawProtoAdapter().decode(reader);
            builder.addUnknownField(tag, fieldEncoding, value);
          }
        }
      }
      reader.endMessage(token);
      return builder.build();
    }

    @Override
    public RepeatedPackedAndMap redact(RepeatedPackedAndMap value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
