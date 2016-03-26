// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: differentpackage/bar.proto at 5:1
package com.squareup.differentpackage.protos.bar;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class Bar extends Message<Bar, Bar.Builder> {
  public static final ProtoAdapter<Bar> ADAPTER = new ProtoAdapter_Bar();

  private static final long serialVersionUID = 0L;

  public Bar() {
    this(ByteString.EMPTY);
  }

  public Bar(ByteString unknownFields) {
    super(ADAPTER, unknownFields);
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Bar;
  }

  @Override
  public int hashCode() {
    return unknownFields().hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    return builder.replace(0, 2, "Bar{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<Bar, Builder> {
    public Builder() {
    }

    public Builder(boolean useDefaults) {
      this();
      if (useDefaults) {
      }
    }

    @Override
    public Bar build() {
      return new Bar(buildUnknownFields());
    }
  }

  public static final class Baz extends Message<Baz, Baz.Builder> {
    public static final ProtoAdapter<Baz> ADAPTER = new ProtoAdapter_Baz();

    private static final long serialVersionUID = 0L;

    public Baz() {
      this(ByteString.EMPTY);
    }

    public Baz(ByteString unknownFields) {
      super(ADAPTER, unknownFields);
    }

    @Override
    public Builder newBuilder() {
      Builder builder = new Builder();
      builder.addUnknownFields(unknownFields());
      return builder;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Baz;
    }

    @Override
    public int hashCode() {
      return unknownFields().hashCode();
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      return builder.replace(0, 2, "Baz{").append('}').toString();
    }

    public static final class Builder extends Message.Builder<Baz, Builder> {
      public Builder() {
      }

      public Builder(boolean useDefaults) {
        this();
        if (useDefaults) {
        }
      }

      @Override
      public Baz build() {
        return new Baz(buildUnknownFields());
      }
    }

    public static final class Moo extends Message<Moo, Moo.Builder> {
      public static final ProtoAdapter<Moo> ADAPTER = new ProtoAdapter_Moo();

      private static final long serialVersionUID = 0L;

      public static final String DEFAULT_BOO = "";

      @WireField(
          tag = 1,
          adapter = "com.squareup.wire.ProtoAdapter#STRING"
      )
      public final String boo;

      public Moo(String boo) {
        this(boo, ByteString.EMPTY);
      }

      public Moo(String boo, ByteString unknownFields) {
        super(ADAPTER, unknownFields);
        this.boo = boo;
      }

      @Override
      public Builder newBuilder() {
        Builder builder = new Builder();
        builder.boo = boo;
        builder.addUnknownFields(unknownFields());
        return builder;
      }

      @Override
      public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Moo)) return false;
        Moo o = (Moo) other;
        return Internal.equals(unknownFields(), o.unknownFields())
            && Internal.equals(boo, o.boo);
      }

      @Override
      public int hashCode() {
        int result = super.hashCode;
        if (result == 0) {
          result = unknownFields().hashCode();
          result = result * 37 + (boo != null ? boo.hashCode() : 0);
          super.hashCode = result;
        }
        return result;
      }

      @Override
      public String toString() {
        StringBuilder builder = new StringBuilder();
        if (boo != null) builder.append(", boo=").append(boo);
        return builder.replace(0, 2, "Moo{").append('}').toString();
      }

      public static final class Builder extends Message.Builder<Moo, Builder> {
        public String boo;

        public Builder() {
        }

        public Builder(boolean useDefaults) {
          this();
          if (useDefaults) {
            boo = DEFAULT_BOO;
          }
        }

        public Builder boo(String boo) {
          this.boo = boo;
          return this;
        }

        @Override
        public Moo build() {
          return new Moo(boo, buildUnknownFields());
        }
      }

      private static final class ProtoAdapter_Moo extends ProtoAdapter<Moo> {
        ProtoAdapter_Moo() {
          super(FieldEncoding.LENGTH_DELIMITED, Moo.class);
        }

        @Override
        public int encodedSize(Moo value) {
          return (value.boo != null ? ProtoAdapter.STRING.encodedSizeWithTag(1, value.boo) : 0)
              + value.unknownFields().size();
        }

        @Override
        public void encode(ProtoWriter writer, Moo value) throws IOException {
          if (value.boo != null) ProtoAdapter.STRING.encodeWithTag(writer, 1, value.boo);
          writer.writeBytes(value.unknownFields());
        }

        @Override
        public Moo decode(ProtoReader reader) throws IOException {
          Builder builder = new Builder();
          long token = reader.beginMessage();
          for (int tag; (tag = reader.nextTag()) != -1;) {
            switch (tag) {
              case 1: builder.boo(ProtoAdapter.STRING.decode(reader)); break;
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
        public Moo redact(Moo value) {
          Builder builder = value.newBuilder();
          builder.clearUnknownFields();
          return builder.build();
        }
      }
    }

    private static final class ProtoAdapter_Baz extends ProtoAdapter<Baz> {
      ProtoAdapter_Baz() {
        super(FieldEncoding.LENGTH_DELIMITED, Baz.class);
      }

      @Override
      public int encodedSize(Baz value) {
        return value.unknownFields().size();
      }

      @Override
      public void encode(ProtoWriter writer, Baz value) throws IOException {
        writer.writeBytes(value.unknownFields());
      }

      @Override
      public Baz decode(ProtoReader reader) throws IOException {
        Builder builder = new Builder();
        long token = reader.beginMessage();
        for (int tag; (tag = reader.nextTag()) != -1;) {
          switch (tag) {
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
      public Baz redact(Baz value) {
        Builder builder = value.newBuilder();
        builder.clearUnknownFields();
        return builder.build();
      }
    }
  }

  private static final class ProtoAdapter_Bar extends ProtoAdapter<Bar> {
    ProtoAdapter_Bar() {
      super(FieldEncoding.LENGTH_DELIMITED, Bar.class);
    }

    @Override
    public int encodedSize(Bar value) {
      return value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, Bar value) throws IOException {
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public Bar decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
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
    public Bar redact(Bar value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
