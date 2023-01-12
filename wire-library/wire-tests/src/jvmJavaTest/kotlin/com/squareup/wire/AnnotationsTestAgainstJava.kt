package com.squareup.wire

import com.squareup.wire.protos.custom_options.EnumOptionOption
import com.squareup.wire.protos.custom_options.EnumValueOptionOption
import com.squareup.wire.protos.custom_options.FooBar
import com.squareup.wire.protos.custom_options.MessageWithOptions
import com.squareup.wire.protos.custom_options.MyFieldOptionFiveOption
import com.squareup.wire.protos.custom_options.MyFieldOptionOneOption
import com.squareup.wire.protos.custom_options.MyFieldOptionSevenOption
import com.squareup.wire.protos.custom_options.MyFieldOptionSixOption
import com.squareup.wire.protos.custom_options.MyFieldOptionThreeOption
import com.squareup.wire.protos.custom_options.MyFieldOptionTwoOption
import com.squareup.wire.protos.custom_options.MyMessageOptionEightOption
import com.squareup.wire.protos.custom_options.MyMessageOptionFourOption
import com.squareup.wire.protos.custom_options.MyMessageOptionNineOption
import com.squareup.wire.protos.custom_options.MyMessageOptionSevenOption
import com.squareup.wire.protos.custom_options.MyMessageOptionTwoOption
import com.squareup.wire.protos.custom_options.RepeatedEnumValueOptionTwoOption
import com.squareup.wire.protos.foreign.ForeignEnum
import com.squareup.wire.protos.foreign.ForeignEnumValueOptionOption
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

// TODO(Benoit) This tests exactly match some others in `AnnotationsTestAgainstKotlin`. We should
//  share the tests and make them run against both java and kotlin; like we do with `jvmJsonTest`.
class AnnotationsTestAgainstJava {
  @Test fun optionsOnMessageTypeWithKotlinReflectApi() {
    val annotations = MessageWithOptions::class.annotations

    val myMessageOptionTwo = annotations
      .filterIsInstance<MyMessageOptionTwoOption>()
      .first()
    assertThat(myMessageOptionTwo.value).isEqualTo(91011.0f)

    val myMessageOptionFour = annotations
      .filterIsInstance<MyMessageOptionFourOption>()
      .first()
    assertThat(myMessageOptionFour.value).isEqualTo(FooBar.FooBarBazEnum.FOO)

    val myMessageOptionSeven = annotations
      .filterIsInstance<MyMessageOptionSevenOption>()
      .first()
    assertThat(myMessageOptionSeven.value).isEqualTo(intArrayOf(33))

    val myMessageOptionEight = annotations
      .filterIsInstance<MyMessageOptionEightOption>()
      .first()
    assertThat(myMessageOptionEight.value).isEqualTo(arrayOf("g", "h"))

    val myMessageOptionNine = annotations
      .filterIsInstance<MyMessageOptionNineOption>()
      .first()
    assertThat(myMessageOptionNine.value).isEqualTo(arrayOf(ForeignEnum.BAV))
  }

  @Test fun optionsOnFieldWithKotlinReflectApi() {
    val property = FooBar::class.members.first { it.name == "foo" }
    val myFieldOptionOne = property.annotations
      .filterIsInstance<MyFieldOptionOneOption>()
      .first()
    assertThat(myFieldOptionOne.value).isEqualTo(17)

    val myFieldOptionTwo = FooBar::class.members.first { it.name == "bar" }.annotations
      .filterIsInstance<MyFieldOptionTwoOption>()
      .first()
    assertThat(myFieldOptionTwo.value).isEqualTo(33.5f)

    val myFieldOptionThree = FooBar::class.members.first { it.name == "baz" }.annotations
      .filterIsInstance<MyFieldOptionThreeOption>()
      .first()
    assertThat(myFieldOptionThree.value).isEqualTo(FooBar.FooBarBazEnum.BAR)

    val quxAnnotations = FooBar::class.members.first { it.name == "qux" }.annotations
    assertThat(quxAnnotations.filterIsInstance<MyFieldOptionFiveOption>().first().value)
      .isEqualTo(intArrayOf(3))
    assertThat(quxAnnotations.filterIsInstance<MyFieldOptionSixOption>().first().value)
      .isEqualTo(arrayOf("a", "b"))
    assertThat(quxAnnotations.filterIsInstance<MyFieldOptionSevenOption>().first().value)
      .isEqualTo(arrayOf(ForeignEnum.BAV, ForeignEnum.BAX))
  }

  @Test fun optionsOnEnumTypeWithKotlinReflectApi() {
    val enumOption = FooBar.FooBarBazEnum::class.annotations
      .filterIsInstance<EnumOptionOption>()
      .first()
    assertThat(enumOption.value).isTrue()
  }

  @Test fun optionsOnMessageTypeWithJavaReflectApi() {
    val annotations = MessageWithOptions::class.java.annotations

    val myMessageOptionTwo = annotations
      .filterIsInstance<MyMessageOptionTwoOption>()
      .first()
    assertThat(myMessageOptionTwo.value).isEqualTo(91011.0f)

    val myMessageOptionFour = annotations
      .filterIsInstance<MyMessageOptionFourOption>()
      .first()
    assertThat(myMessageOptionFour.value).isEqualTo(FooBar.FooBarBazEnum.FOO)

    val myMessageOptionSeven = annotations
      .filterIsInstance<MyMessageOptionSevenOption>()
      .first()
    assertThat(myMessageOptionSeven.value).isEqualTo(intArrayOf(33))

    val myMessageOptionEight = annotations
      .filterIsInstance<MyMessageOptionEightOption>()
      .first()
    assertThat(myMessageOptionEight.value).isEqualTo(arrayOf("g", "h"))

    val myMessageOptionNine = annotations
      .filterIsInstance<MyMessageOptionNineOption>()
      .first()
    assertThat(myMessageOptionNine.value).isEqualTo(arrayOf(ForeignEnum.BAV))
  }

  @Test fun optionsOnFieldWithJavaReflectApi() {
    val myFieldOptionOne = FooBar::class.java.getField("foo").annotations
      .filterIsInstance<MyFieldOptionOneOption>()
      .first()
    assertThat(myFieldOptionOne.value).isEqualTo(17)

    val myFieldOptionTwo = FooBar::class.java.getField("bar").annotations
      .filterIsInstance<MyFieldOptionTwoOption>()
      .first()
    assertThat(myFieldOptionTwo.value).isEqualTo(33.5f)

    val myFieldOptionThree = FooBar::class.java.getField("baz").annotations
      .filterIsInstance<MyFieldOptionThreeOption>()
      .first()
    assertThat(myFieldOptionThree.value).isEqualTo(FooBar.FooBarBazEnum.BAR)

    val quxAnnotations = FooBar::class.java.getField("qux").annotations
    assertThat(quxAnnotations.filterIsInstance<MyFieldOptionFiveOption>().first().value)
      .isEqualTo(intArrayOf(3))
    assertThat(quxAnnotations.filterIsInstance<MyFieldOptionSixOption>().first().value)
      .isEqualTo(arrayOf("a", "b"))
    assertThat(quxAnnotations.filterIsInstance<MyFieldOptionSevenOption>().first().value)
      .isEqualTo(arrayOf(ForeignEnum.BAV, ForeignEnum.BAX))
  }

  @Test fun optionsOnEnumTypeWithJavaReflectApi() {
    val enumOption = FooBar.FooBarBazEnum::class.java.annotations
      .filterIsInstance<EnumOptionOption>()
      .first()
    assertThat(enumOption.value).isTrue()
  }

  // Note that Benoît didn't find any Kotlin API equivalent to do this.
  @Test fun optionsOnEnumConstantsWithJavaReflectApi() {
    val enumValueOption = FooBar.FooBarBazEnum::class.java.getField("FOO").annotations
      .filterIsInstance<EnumValueOptionOption>()
      .first()
    assertThat(enumValueOption.value).isEqualTo(17)

    val foreignEnumValueOption = FooBar.FooBarBazEnum::class.java.getField("BAR").annotations
      .filterIsInstance<ForeignEnumValueOptionOption>()
      .first()
    assertThat(foreignEnumValueOption.value).isTrue()

    val bazOptions = FooBar.FooBarBazEnum::class.java.getField("BAZ").annotations
    assertThat(bazOptions.filterIsInstance<EnumValueOptionOption>().first().value).isEqualTo(18)
    assertThat(bazOptions.filterIsInstance<ForeignEnumValueOptionOption>().first().value).isFalse()
    assertThat(bazOptions.filterIsInstance<RepeatedEnumValueOptionTwoOption>().first().value)
      .isEqualTo(arrayOf("c", "d"))
  }
}
