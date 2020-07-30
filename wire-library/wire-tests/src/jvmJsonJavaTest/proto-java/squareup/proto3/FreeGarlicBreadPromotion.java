// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.proto3.FreeGarlicBreadPromotion in pizza.proto
package squareup.proto3;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.Syntax;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import okio.ByteString;

public final class FreeGarlicBreadPromotion extends Message<FreeGarlicBreadPromotion, FreeGarlicBreadPromotion.Builder> {
  public static final ProtoAdapter<FreeGarlicBreadPromotion> ADAPTER = ProtoAdapter.newMessageAdapter(FreeGarlicBreadPromotion.class, "type.googleapis.com/squareup.proto3.FreeGarlicBreadPromotion", Syntax.PROTO_3);

  private static final long serialVersionUID = 0L;

  public static final Boolean DEFAULT_IS_EXTRA_CHEESEY = false;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#BOOL",
      label = WireField.Label.OMIT_IDENTITY,
      jsonName = "isExtraCheesey"
  )
  public final Boolean is_extra_cheesey;

  public FreeGarlicBreadPromotion(Boolean is_extra_cheesey) {
    this(is_extra_cheesey, ByteString.EMPTY);
  }

  public FreeGarlicBreadPromotion(Boolean is_extra_cheesey, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.is_extra_cheesey = is_extra_cheesey;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.is_extra_cheesey = is_extra_cheesey;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof FreeGarlicBreadPromotion)) return false;
    FreeGarlicBreadPromotion o = (FreeGarlicBreadPromotion) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(is_extra_cheesey, o.is_extra_cheesey);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (is_extra_cheesey != null ? is_extra_cheesey.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<FreeGarlicBreadPromotion, Builder> {
    public Boolean is_extra_cheesey;

    public Builder() {
    }

    public Builder is_extra_cheesey(Boolean is_extra_cheesey) {
      this.is_extra_cheesey = is_extra_cheesey;
      return this;
    }

    @Override
    public FreeGarlicBreadPromotion build() {
      return new FreeGarlicBreadPromotion(is_extra_cheesey, super.buildUnknownFields());
    }
  }
}
