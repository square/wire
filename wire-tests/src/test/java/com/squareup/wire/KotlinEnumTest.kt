package com.squareup.wire

import com.squareup.wire.protos.kotlin.person.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class KotlinEnumTest {
  @Test fun getValue() {
    assertThat(Person.PhoneType.HOME.value).isEqualTo(1)
  }

  @Test fun fromValue() {
    assertThat(Person.PhoneType.fromValue(1)).isEqualTo(Person.PhoneType.HOME)
  }
}
