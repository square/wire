// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.extension_collision.CollisionSubject in extension_collision.proto
package squareup.protos.extension_collision;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.ReverseProtoWriter;
import com.squareup.wire.Syntax;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;
import squareup.protos.extension_collision_1.AOption;
import squareup.protos.extension_collision_1.BOption;
import squareup.protos.extension_collision_2.COption;

public final class CollisionSubject extends Message<CollisionSubject, CollisionSubject.Builder> {
  public static final ProtoAdapter<CollisionSubject> ADAPTER = new ProtoAdapter_CollisionSubject();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_F = "";

  @AOption("1a")
  @BOption("1b")
  @squareup.protos.extension_collision_2.AOption("2a")
  @COption("2c")
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String f;

  public CollisionSubject(String f) {
    this(f, ByteString.EMPTY);
  }

  public CollisionSubject(String f, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.f = f;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.f = f;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof CollisionSubject)) return false;
    CollisionSubject o = (CollisionSubject) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(f, o.f);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (f != null ? f.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (f != null) builder.append(", f=").append(Internal.sanitize(f));
    return builder.replace(0, 2, "CollisionSubject{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<CollisionSubject, Builder> {
    public String f;

    public Builder() {
    }

    public Builder f(String f) {
      this.f = f;
      return this;
    }

    @Override
    public CollisionSubject build() {
      return new CollisionSubject(f, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_CollisionSubject extends ProtoAdapter<CollisionSubject> {
    public ProtoAdapter_CollisionSubject() {
      super(FieldEncoding.LENGTH_DELIMITED, CollisionSubject.class, "type.googleapis.com/squareup.protos.extension_collision.CollisionSubject", Syntax.PROTO_2, null);
    }

    @Override
    public int encodedSize(CollisionSubject value) {
      int result = 0;
      result += ProtoAdapter.STRING.encodedSizeWithTag(1, value.f);
      result += value.unknownFields().size();
      return result;
    }

    @Override
    public void encode(ProtoWriter writer, CollisionSubject value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.f);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public void encode(ReverseProtoWriter writer, CollisionSubject value) throws IOException {
      writer.writeBytes(value.unknownFields());
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.f);
    }

    @Override
    public CollisionSubject decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.f(ProtoAdapter.STRING.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public CollisionSubject redact(CollisionSubject value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
