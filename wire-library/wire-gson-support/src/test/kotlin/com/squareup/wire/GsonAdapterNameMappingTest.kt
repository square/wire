package com.squareup.wire.protos

import com.google.gson.GsonBuilder
import com.squareup.wire.WireTypeAdapterFactory
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat

class GsonAdapterNameMappingTest {
    private val gson = GsonBuilder()
            .registerTypeAdapterFactory(WireTypeAdapterFactory())
            .disableHtmlEscaping()
            .create()

    @Test
    fun testSerializingHappyTest() {
        val airportLocation = AirportLocation("Sheremetyevo", "Moscow", "Russia")

        val result = gson.toJson(airportLocation)
        assertThat(result).isEqualTo(SVO_AIRPORT_SERVER_JSON)
    }

    @Test
    fun testDeserializingHappyTest() {
        val targetAirportLocation = AirportLocation("Sheremetyevo", "Moscow", "Russia")
        val airportLocation = gson.fromJson(SVO_AIRPORT_SERVER_JSON, AirportLocation::class.java)

        assertThat(targetAirportLocation).isEqualTo(airportLocation)
    }
}

private const val SVO_AIRPORT_SERVER_JSON = "{\"airportName\":\"Sheremetyevo\",\"cityName\":\"Moscow\",\"countryName\":\"Russia\"}"