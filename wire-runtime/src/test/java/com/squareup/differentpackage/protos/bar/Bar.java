// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/differentpackage/bar.proto at 5:1
package com.squareup.differentpackage.protos.bar;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.TagMap;
import com.squareup.wire.WireField;
import java.io.ObjectStreamException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public final class Bar extends Message<Bar> {
  public static final ProtoAdapter<Bar> ADAPTER = ProtoAdapter.newMessageAdapter(Bar.class);

  private static final long serialVersionUID = 0L;

  public Bar() {
    this(null);
  }

  public Bar(TagMap tagMap) {
    super(tagMap);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Bar;
  }

  @Override
  public int hashCode() {
    return tagMap() != null ? tagMap().hashCode() : 0;
  }

  private Object writeReplace() throws ObjectStreamException {
    return super.createSerializedForm();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<Bar, Builder> {
    public Builder() {
    }

    public Builder(Bar message) {
      super(message);
    }

    @Override
    public Bar build() {
      return new Bar(buildTagMap());
    }
  }

  public static final class Baz extends Message<Baz> {
    public static final ProtoAdapter<Baz> ADAPTER = ProtoAdapter.newMessageAdapter(Baz.class);

    private static final long serialVersionUID = 0L;

    public Baz() {
      this(null);
    }

    public Baz(TagMap tagMap) {
      super(tagMap);
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Baz;
    }

    @Override
    public int hashCode() {
      return tagMap() != null ? tagMap().hashCode() : 0;
    }

    private Object writeReplace() throws ObjectStreamException {
      return super.createSerializedForm();
    }

    public static final class Builder extends com.squareup.wire.Message.Builder<Baz, Builder> {
      public Builder() {
      }

      public Builder(Baz message) {
        super(message);
      }

      @Override
      public Baz build() {
        return new Baz(buildTagMap());
      }
    }

    public static final class Moo extends Message<Moo> {
      public static final ProtoAdapter<Moo> ADAPTER = ProtoAdapter.newMessageAdapter(Moo.class);

      private static final long serialVersionUID = 0L;

      public static final String DEFAULT_BOO = "";

      @WireField(
          tag = 1,
          adapter = "com.squareup.wire.ProtoAdapter#STRING"
      )
      public final String boo;

      public Moo(String boo) {
        this(boo, null);
      }

      public Moo(String boo, TagMap tagMap) {
        super(tagMap);
        this.boo = boo;
      }

      @Override
      public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Moo)) return false;
        Moo o = (Moo) other;
        return equals(tagMap(), o.tagMap())
            && equals(boo, o.boo);
      }

      @Override
      public int hashCode() {
        int result = hashCode;
        if (result == 0) {
          result = tagMap() != null ? tagMap().hashCode() : 0;
          result = result * 37 + (boo != null ? boo.hashCode() : 0);
          hashCode = result;
        }
        return result;
      }

      private Object writeReplace() throws ObjectStreamException {
        return super.createSerializedForm();
      }

      public static final class Builder extends com.squareup.wire.Message.Builder<Moo, Builder> {
        public String boo;

        public Builder() {
        }

        public Builder(Moo message) {
          super(message);
          if (message == null) return;
          this.boo = message.boo;
        }

        public Builder boo(String boo) {
          this.boo = boo;
          return this;
        }

        @Override
        public Moo build() {
          return new Moo(boo, buildTagMap());
        }
      }
    }
  }
}
