package com.geepscontrol.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class WeatherForecastTest {
    private val response = """{
        "timezone":"Asia/Tokyo",
        "current":{"time":1793833200,"temperature_2m":16.4,"apparent_temperature":15.1,"weather_code":2,"relative_humidity_2m":70,"wind_speed_10m":12},
        "hourly":{"time":[1793826000,1793829600,1793833200,1793836800],"temperature_2m":[14,15,null,18],"weather_code":[1,2,3,61],"precipitation_probability":[0,10,20,75]},
        "daily":{"time":[1793804400],"temperature_2m_max":[21],"temperature_2m_min":[12],"weather_code":[61],"precipitation_probability_max":[75],"sunrise":[1793826000],"sunset":[1793865600],"uv_index_max":[4.5]}
    }"""

    @Test fun `parses current daily and upcoming hourly weather in forecast zone`() {
        val forecast = WeatherForecastParser.parse(response, "Current location", 1234)
        assertEquals("Asia/Tokyo", forecast.timezone)
        assertEquals(16.4, forecast.temperature)
        assertEquals(1793833200000L, forecast.currentAt)
        assertEquals(1234L, forecast.fetchedAt)
        assertEquals(3, forecast.hours.size)
        assertNull(forecast.hours[1].temperature)
        assertEquals(75.0, forecast.days.single().rainChance)
        assertEquals(1793826000000L, forecast.days.single().sunrise)
    }

    @Test fun `missing optional weather fields display safely`() {
        val forecast = WeatherForecastParser.parse("""{"current":{"time":1,"temperature_2m":0}}""", "City", 1)
        assertEquals(emptyList(), forecast.hours)
        assertEquals(emptyList(), forecast.days)
        assertNull(forecast.humidity)
        assertEquals("Conditions unavailable", WeatherPresentation.description(forecast.code))
        assertEquals("—", WeatherPresentation.temperature(forecast.feelsLike, false))
    }

    @Test fun `missing required current weather is rejected`() {
        assertFails { WeatherForecastParser.parse("{}", "City", 1) }
        assertFails { WeatherForecastParser.parse("""{"current":{"time":1,"temperature_2m":null}}""", "City", 1) }
    }

    @Test fun `weather temperature unit conversion handles freezing and negative temperatures`() {
        assertEquals("32°F", WeatherPresentation.temperature(0.0, true))
        assertEquals("-40°F", WeatherPresentation.temperature(-40.0, true))
        assertEquals("16°C", WeatherPresentation.temperature(16.4, false))
    }

    @Test fun `geocoding distinguishes same named cities and ignores invalid coordinates`() {
        val places = WeatherForecastParser.places("""{"results":[{"name":"Springfield","admin1":"Illinois","country":"United States","latitude":39.8,"longitude":-89.6},{"name":"Springfield","admin1":"Massachusetts","country":"United States","latitude":42.1,"longitude":-72.6},{"name":"Broken","latitude":1000,"longitude":1}]}""")
        assertEquals(2, places.size)
        assertEquals("Springfield, Illinois, United States", places[0].name)
        assertEquals("Springfield, Massachusetts, United States", places[1].name)
        assertEquals(emptyList(), WeatherForecastParser.places("{}"))
    }
}
