/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.moshi.Moshi
import org.junit.Test
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
