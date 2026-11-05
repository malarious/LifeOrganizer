package com.geepscontrol.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import android.util.AtomicFile
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.geepscontrol.core.domain.WeatherForecast
import com.geepscontrol.core.domain.WeatherForecastParser
import com.geepscontrol.core.domain.WeatherPlace
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.math.round

class AndroidWeatherRepository(
    private val context: Context,
) {
    private companion object {
        val cacheLock = Any()
    }

    private val preferences = context.getSharedPreferences("lifeminder-weather", Context.MODE_PRIVATE)
    private val cache = AtomicFile(File(context.noBackupFilesDir, "weather.json"))
    var enabled: Boolean
        get() = preferences.getBoolean("enabled", false)
        set(value) {
            preferences.edit().putBoolean("enabled", value).apply()
        }
    var afterAlarm: Boolean
        get() = preferences.getBoolean("after_alarm", true)
        set(value) {
            preferences.edit().putBoolean("after_alarm", value).apply()
        }
    var fahrenheit: Boolean
        get() =
            preferences.getBoolean(
                "fahrenheit",
                java.util.Locale
                    .getDefault()
                    .country == "US",
            )
        set(value) {
            preferences.edit().putBoolean("fahrenheit", value).apply()
        }

    fun selectedPlace(): WeatherPlace? = preferences.getString("place", null)?.let { text -> runCatching { JSONObject(text).toPlace() }.getOrNull() }

    fun select(place: WeatherPlace?) {
        preferences
            .edit()
            .putString(
                "place",
                place?.let {
                    JSONObject()
                        .put("latitude", it.latitude)
                        .put("longitude", it.longitude)
                        .put("name", it.name)
                        .toString()
                },
            ).apply()
        enabled = true
    }

    fun turnOff() {
        enabled = false
        afterAlarm = false
        preferences.edit().remove("place").apply()
        synchronized(cacheLock) { cache.delete() }
    }

    suspend fun cached(): WeatherForecast? =
        withContext(Dispatchers.IO) {
            if (!enabled) return@withContext null
            runCatching {
                synchronized(cacheLock) {
                    val value = JSONObject(cache.openRead().bufferedReader().use { it.readText() })
                    WeatherForecastParser.parse(value.getString("payload"), value.getString("name"), value.getLong("fetchedAt"))
                }
            }.getOrNull()
        }

    suspend fun search(query: String): List<WeatherPlace> =
        withContext(Dispatchers.IO) {
            require(query.trim().length >= 2) { "Enter at least two letters for a city" }
            WeatherForecastParser.places(get("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query.trim(), "UTF-8")}&count=5&language=en&format=json"))
        }

    suspend fun forecast(place: WeatherPlace): WeatherForecast =
        withContext(Dispatchers.IO) {
            // Two decimal places avoid transmitting a precise device fix to the weather provider.
            val lat = round(place.latitude * 100) / 100
            val lon = round(place.longitude * 100) / 100
            require(lat in -90.0..90.0 && lon in -180.0..180.0) { "Location is invalid" }
            val payload = get("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&timezone=auto&timeformat=unixtime&forecast_days=7&current=temperature_2m,apparent_temperature,weather_code,relative_humidity_2m,wind_speed_10m&hourly=temperature_2m,weather_code,precipitation_probability&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset,uv_index_max")
            val fetchedAt = System.currentTimeMillis()
            val parsed = WeatherForecastParser.parse(payload, place.name, fetchedAt)
            val bytes =
                JSONObject()
                    .put("payload", payload)
                    .put("name", place.name)
                    .put("fetchedAt", fetchedAt)
                    .toString()
                    .toByteArray(Charsets.UTF_8)
            // A cache-write failure must not hide a successfully downloaded forecast.
            runCatching {
                synchronized(cacheLock) {
                    if (!enabled) return@synchronized
                    val stream = cache.startWrite()
                    try {
                        stream.write(bytes)
                        cache.finishWrite(stream)
                    } catch (failure: Exception) {
                        cache.failWrite(stream)
                        throw failure
                    }
                }
            }
            parsed
        }

    @SuppressLint("MissingPermission")
    suspend fun currentPlace(): WeatherPlace {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) { "Allow approximate location, or search for a city below" }
        val manager = context.getSystemService(LocationManager::class.java)
        check(LocationManagerCompat.isLocationEnabled(manager)) { "Location is off. Turn it on in Android settings, or search for a city" }
        val provider =
            listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).firstOrNull { it in manager.allProviders && manager.isProviderEnabled(it) }
                ?: error("No location provider is available. Search for a city instead")
        val location =
            withTimeoutOrNull(15_000L) {
                suspendCancellableCoroutine<android.location.Location?> { continuation ->
                    val cancellation = CancellationSignal()
                    continuation.invokeOnCancellation { cancellation.cancel() }
                    LocationManagerCompat.getCurrentLocation(manager, provider, cancellation, Executor { it.run() }) { fix ->
                        if (continuation.isActive) continuation.resume(fix)
                    }
                }
            } ?: error("Could not get your location. Retry outdoors, or search for a city")
        check(SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos <= 10 * 60 * 1_000_000_000L) { "The available location is old. Retry or choose a city" }
        return WeatherPlace(location.latitude, location.longitude, "Current location (approximate)", fromDevice = true)
    }

    private suspend fun get(address: String): String =
        coroutineScope {
            val connection = URL(address).openConnection() as HttpURLConnection
            val cancellation =
                launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        connection.disconnect()
                    }
                }
            try {
                withContext(Dispatchers.IO) {
                    ensureActive()
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", "LifeMinder/0.1 (personal weather)")
                    val status = connection.responseCode
                    if (status == 429) throw IOException("Weather is busy. Please try again in a few minutes")
                    if (status !in 200..299) throw IOException("Weather service is unavailable. Please try again later")
                    connection.inputStream.bufferedReader().use { reader ->
                        val content = StringBuilder()
                        val buffer = CharArray(4096)
                        while (true) {
                            ensureActive()
                            val count = reader.read(buffer)
                            if (count < 0) break
                            check(content.length + count <= 1_000_000) { "Weather response is too large" }
                            content.append(buffer, 0, count)
                        }
                        content.toString()
                    }
                }
            } finally {
                cancellation.cancel()
                connection.disconnect()
            }
        }

    private fun JSONObject.toPlace() = WeatherPlace(getDouble("latitude"), getDouble("longitude"), getString("name"))
}
