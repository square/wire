// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: roots.proto at 51:1
package com.squareup.wire.protos.roots;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.TagMap;
import com.squareup.wire.WireField;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;

public final class E extends Message<E> {
  public static final ProtoAdapter<E> ADAPTER = ProtoAdapter.newMessageAdapter(E.class);

  private static final long serialVersionUID = 0L;

  public static final G DEFAULT_G = G.FOO;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.protos.roots.E$F#ADAPTER"
  )
  public final F f;

  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.protos.roots.G#ADAPTER"
  )
  public final G g;

  public E(F f, G g) {
    this(f, g, TagMap.EMPTY);
  }

  public E(F f, G g, TagMap tagMap) {
    super(tagMap);
    this.f = f;
    this.g = g;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof E)) return false;
    E o = (E) other;
    return equals(tagMap(), o.tagMap())
        && equals(f, o.f)
        && equals(g, o.g);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = tagMap().hashCode();
      result = result * 37 + (f != null ? f.hashCode() : 0);
      result = result * 37 + (g != null ? g.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<E, Builder> {
    public F f;

    public G g;

    public Builder() {
    }

    public Builder(E message) {
      super(message);
      if (message == null) return;
      this.f = message.f;
      this.g = message.g;
    }

    public Builder f(F f) {
      this.f = f;
      return this;
    }

    public Builder g(G g) {
      this.g = g;
      return this;
    }

    @Override
    public E build() {
      return new E(f, g, buildTagMap());
    }
  }

  public static final class F extends Message<F> {
    public static final ProtoAdapter<F> ADAPTER = ProtoAdapter.newMessageAdapter(F.class);

    private static final long serialVersionUID = 0L;

    public static final Integer DEFAULT_I = 0;

    @WireField(
        tag = 1,
        adapter = "com.squareup.wire.ProtoAdapter#INT32"
    )
    public final Integer i;

    public F(Integer i) {
      this(i, TagMap.EMPTY);
    }

    public F(Integer i, TagMap tagMap) {
      super(tagMap);
      this.i = i;
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) return true;
      if (!(other instanceof F)) return false;
      F o = (F) other;
      return equals(tagMap(), o.tagMap())
          && equals(i, o.i);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode;
      if (result == 0) {
        result = tagMap().hashCode();
        result = result * 37 + (i != null ? i.hashCode() : 0);
        super.hashCode = result;
      }
      return result;
    }

    public static final class Builder extends com.squareup.wire.Message.Builder<F, F.Builder> {
      public Integer i;

      public Builder() {
      }

      public Builder(F message) {
        super(message);
        if (message == null) return;
        this.i = message.i;
      }

      public Builder i(Integer i) {
        this.i = i;
        return this;
      }

      @Override
      public F build() {
        return new F(i, buildTagMap());
      }
    }
  }
}
