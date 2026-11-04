package com.geepscontrol.core.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

data class WeatherPlace(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val fromDevice: Boolean = false,
)

data class WeatherHour(
    val at: Long,
    val temperature: Double?,
    val code: Int?,
    val rainChance: Double?,
)

data class WeatherDay(
    val at: Long,
    val code: Int?,
    val high: Double?,
    val low: Double?,
    val rainChance: Double?,
    val sunrise: Long?,
    val sunset: Long?,
    val uv: Double?,
)

data class WeatherForecast(
    val place: String,
    val fetchedAt: Long,
    val currentAt: Long,
    val timezone: String,
    val temperature: Double,
    val feelsLike: Double?,
    val code: Int?,
    val humidity: Double?,
    val windKmh: Double?,
    val hours: List<WeatherHour>,
    val days: List<WeatherDay>,
)

object WeatherForecastParser {
    fun parse(
        content: String,
        place: String,
        fetchedAt: Long,
    ): WeatherForecast {
        val root = Json.parseToJsonElement(content) as? JsonObject ?: error("Weather response was not an object")
        val current = root["current"] as? JsonObject ?: error("Current conditions are unavailable")
        val hourly = root["hourly"] as? JsonObject
        val daily = root["daily"] as? JsonObject
        val currentAt = current.number("time")?.toLong()?.times(1000L) ?: error("Weather time is missing")
        val hours = hourly?.times()?.mapIndexed { i, at -> WeatherHour(at, hourly.numberAt("temperature_2m", i), hourly.numberAt("weather_code", i)?.toInt(), hourly.numberAt("precipitation_probability", i)) }.orEmpty()
        val days = daily?.times()?.mapIndexed { i, at -> WeatherDay(at, daily.numberAt("weather_code", i)?.toInt(), daily.numberAt("temperature_2m_max", i), daily.numberAt("temperature_2m_min", i), daily.numberAt("precipitation_probability_max", i), daily.numberAt("sunrise", i)?.toLong()?.times(1000L), daily.numberAt("sunset", i)?.toLong()?.times(1000L), daily.numberAt("uv_index_max", i)) }.orEmpty()
        return WeatherForecast(
            place,
            fetchedAt,
            currentAt,
            (root["timezone"] as? JsonPrimitive)?.contentOrNull ?: "UTC",
            current.number("temperature_2m") ?: error("Current temperature is unavailable"),
            current.number("apparent_temperature"),
            current.number("weather_code")?.toInt(),
            current.number("relative_humidity_2m"),
            current.number("wind_speed_10m"),
            hours.filter { it.at >= currentAt - 3_600_000L }.take(12),
            days.take(7),
        )
    }

    fun places(content: String): List<WeatherPlace> {
        val root = Json.parseToJsonElement(content) as? JsonObject ?: return emptyList()
        return (root["results"] as? JsonArray)
            ?.mapNotNull { row ->
                val item = row as? JsonObject ?: return@mapNotNull null
                val lat = item.number("latitude") ?: return@mapNotNull null
                val lon = item.number("longitude") ?: return@mapNotNull null
                val label = listOf("name", "admin1", "country").mapNotNull { (item[it] as? JsonPrimitive)?.contentOrNull }.distinct().joinToString(", ")
                if (lat !in -90.0..90.0 || lon !in -180.0..180.0 || label.isBlank()) null else WeatherPlace(lat, lon, label)
            }.orEmpty()
    }

    private fun JsonObject.number(key: String) = (this[key] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }

    private fun JsonObject.numberAt(
        key: String,
        index: Int,
    ) = ((this[key] as? JsonArray)?.getOrNull(index) as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() }

    private fun JsonObject.times() = (this["time"] as? JsonArray)?.map { ((it as? JsonPrimitive)?.longOrNull ?: error("Forecast time is missing")) * 1000L }.orEmpty()
}

object WeatherPresentation {
    fun temperature(
        celsius: Double?,
        fahrenheit: Boolean,
    ): String = celsius?.let { "${kotlin.math.round(if (fahrenheit) it * 9 / 5 + 32 else it).toInt()}°${if (fahrenheit) "F" else "C"}" } ?: "—"

    fun description(code: Int?): String =
        when (code) {
            0 -> "Clear sky"
            1 -> "Mostly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorms"
            96, 99 -> "Thunderstorms with hail"
            else -> "Conditions unavailable"
        }
}
