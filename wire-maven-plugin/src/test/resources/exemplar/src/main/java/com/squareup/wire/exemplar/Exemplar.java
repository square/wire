package com.squareup.wire.exemplar;

import com.squareup.protos.wire.exemplar.AbstractLocaleAdapter;
import com.squareup.protos.wire.exemplar.MuchAwesome;
import com.squareup.wire.ProtoAdapter;
import java.io.IOException;
import java.util.Locale;

public class Exemplar {
  public static final ProtoAdapter<Locale> LOCALE_ADAPTER = new AbstractLocaleAdapter() {
    @Override public Locale fromProto(String language, String country, String variant) {
      return new Locale(language, country, variant);
    }
    @Override public String country(Locale locale) {
      return locale.getCountry();
    }
    @Override public String language(Locale locale) {
      return locale.getLanguage();
    }
    @Override public String variant(Locale locale) {
      return locale.getVariant();
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
