package com.squareup.wire.exemplar;

import com.squareup.protos.wire.exemplar.MuchAwesome;

public class Exemplar {

  public static void main(String[] args) {
    MuchAwesome doge = new MuchAwesome.Builder().very_proto("Wow!").build();
    System.out.println(doge.toString());
  }
}
