package com.squareup.wire

import com.squareup.moshi.Moshi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import com.squareup.wire.protos.person.java.Person as JavaPerson
import com.squareup.wire.protos.person.javainteropkotlin.Person as JavaInteropKotlinPerson
import com.squareup.wire.protos.person.kotlin.Person as KotlinPerson

class MoshiPersonTest {
  /**
   * When we encounter an explicit null in the JSON, we discard it. As a consequence we won't
   * overwrite a non-null value with null which could happen in malformed JSON that repeats a value.
   */
  @Test
  fun javaClobberNonNullWithNull() {
    val personWithName = moshi.adapter(JavaPerson::class.java)
        .fromJson("""{"id":1,"name":"Jo","email":"foo@square.com"}""")
    assertThat(personWithName!!.email).isEqualTo("foo@square.com")

    val personWithNameClobberedWithNull = moshi.adapter(JavaPerson::class.java)
        .fromJson("""{"id":1,"name":"Jo","email":"foo@square.com","email":null}""")
    assertThat(personWithNameClobberedWithNull!!.email).isEqualTo("foo@square.com")
  }

  @Test @Ignore("The adapter doesn't support idiomatic Kotlin yet.")
  fun kotlinClobberNonNullWithNull() {
    val personWithName = moshi.adapter(KotlinPerson::class.java)
        .fromJson("""{"id":1,"name":"Jo","email":"foo@square.com"}""")
    assertThat(personWithName!!.email).isEqualTo("foo@square.com")

    val personWithNameClobberedWithNull = moshi.adapter(KotlinPerson::class.java)
        .fromJson("""{"id":1,"name":"Jo","email":"foo@square.com","email":null}""")
    assertThat(personWithNameClobberedWithNull!!.email).isEqualTo("foo@square.com")
  }

  @Test
  fun javaInteropKotlinClobberNonNullWithNull() {
    val personWithName = moshi.adapter(JavaInteropKotlinPerson::class.java)
        .fromJson("{\"id\":1,\"name\":\"Jo\",\"email\":\"foo@square.com\"}")
    assertThat(personWithName!!.email).isEqualTo("foo@square.com")

    val personWithNameClobberedWithNull = moshi.adapter(JavaInteropKotlinPerson::class.java)
        .fromJson("{\"id\":1,\"name\":\"Jo\",\"email\":\"foo@square.com\",\"email\":null}")
    assertThat(personWithNameClobberedWithNull!!.email).isEqualTo("foo@square.com")
  }

  companion object {
    private val moshi = Moshi.Builder()
        .add(WireJsonAdapterFactory())
        .build()
  }
}