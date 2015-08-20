package com.squareup.wire.exemplar;

import com.squareup.protos.wire.exemplar.MuchAwesome;
import com.squareup.protos.wire.exemplar.MuchAwesome2;

public class Exemplar {

  public static void main(String[] args) {
    MuchAwesome doge = new MuchAwesome.Builder().very_proto("Wow!").build();
    System.out.println(doge.toString());

    MuchAwesome2 doge2 = new MuchAwesome.Builder().very_proto2("Wow!").build();
    System.out.println(doge.toString());
  }
}
