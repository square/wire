package com.squareup.wire.exemplar;

import com.squareup.protos.wire.exemplar.LocaleProtoFields;
import com.squareup.protos.wire.exemplar.MuchAwesome;
import com.squareup.wire.FieldEncoding;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import java.io.IOException;
import java.util.Locale;

public class Exemplar {
  /** A handwritten adapter for Java's built-in locale class. */
  public static final ProtoAdapter<Locale> LOCALE_ADAPTER
      = new ProtoAdapter<Locale>(FieldEncoding.LENGTH_DELIMITED, Locale.class) {
    @Override public Locale decode(ProtoReader reader) throws IOException {
      long token = reader.beginMessage();
      String language = null;
      String country = null;
      String variant = null;
      for (int tag; (tag = reader.nextTag()) != -1;) {
        if (tag == LocaleProtoFields.language.tag) {
          language = LocaleProtoFields.language.protoAdapter.decode(reader);
        } else if (tag == LocaleProtoFields.country.tag) {
          country = LocaleProtoFields.country.protoAdapter.decode(reader);
        } else if (tag == LocaleProtoFields.variant.tag) {
          variant = LocaleProtoFields.variant.protoAdapter.decode(reader);
        } else {
          reader.skip();
        }
      }
      reader.endMessage(token);
      return new Locale(language, country, variant);
    }

    @Override public int encodedSize(Locale value) {
      return LocaleProtoFields.language.encodedSize(value.getLanguage())
          + LocaleProtoFields.country.encodedSize(value.getCountry())
          + LocaleProtoFields.variant.encodedSize(value.getVariant());
    }

    @Override public void encode(ProtoWriter writer, Locale value) throws IOException {
      LocaleProtoFields.language.encode(writer, value.getLanguage());
      LocaleProtoFields.country.encode(writer, value.getCountry());
      LocaleProtoFields.variant.encode(writer, value.getVariant());
    }
  };

  public static void main(String[] args) throws IOException {
    MuchAwesome doge = new MuchAwesome.Builder()
        .very_proto("Wow!")
        .locale(Locale.CANADA_FRENCH)
        .build();

    byte[] dogeBytes = doge.encode();
    MuchAwesome anotherDoge = MuchAwesome.ADAPTER.decode(dogeBytes);

    System.out.println(anotherDoge);
  }
}
