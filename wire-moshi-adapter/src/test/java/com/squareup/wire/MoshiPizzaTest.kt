package com.squareup.wire

import com.squareup.moshi.Moshi
import com.squareup.wire.json.assertJsonEquals
import com.squareup.wire.proto2.kotlin.Getters
import com.squareup.wire.proto2.person.kotlin.Person.PhoneNumber
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import com.squareup.wire.proto2.person.java.Person as JavaPerson
import com.squareup.wire.proto2.person.javainteropkotlin.Person as JavaInteropKotlinPerson
import com.squareup.wire.proto2.person.kotlin.Person as KotlinPerson
import squareup.proto3.BuyOneGetOnePromotion

/* Pizza is a proto3 generated type. */
class MoshiPizzaTest {
  @Test
  fun kotlinBuyOneGetOnePromotionFromJsonWithValue() {
    val promotion = moshi.adapter(BuyOneGetOnePromotion::class.java)
      .fromJson("""{"coupon":"36OFF"}""")
    assertThat(promotion).isEqualTo(BuyOneGetOnePromotion(coupon = "36OFF"))
  }

  @Test
  fun kotlinBuyOneGetOnePromotionToJsonWithValue() {
    val promotion = moshi.adapter(BuyOneGetOnePromotion::class.java)
      .toJson(BuyOneGetOnePromotion(coupon = "36OFF"))
    assertThat(promotion).isEqualTo("""{"coupon":"36OFF"}""")
  }

  @Test
  fun kotlinBuyOneGetOnePromotionFromJsonWithIdentity() {
    val promotion = moshi.adapter(BuyOneGetOnePromotion::class.java)
      .fromJson("""{}""")
    assertThat(promotion).isEqualTo(BuyOneGetOnePromotion())
  }

  @Test
  fun kotlinBuyOneGetOnePromotionToJsonWithIdentity() {
    val promotion = moshi.adapter(BuyOneGetOnePromotion::class.java)
      .toJson(BuyOneGetOnePromotion())
    assertThat(promotion).isEqualTo("""{}""")
  }

  companion object {
    private val moshi = Moshi.Builder()
      .add(WireJsonAdapterFactory())
      .build()
  }
}
