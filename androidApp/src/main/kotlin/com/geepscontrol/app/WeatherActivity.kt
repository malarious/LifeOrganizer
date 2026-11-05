package com.geepscontrol.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.geepscontrol.core.domain.WeatherPlace
import com.geepscontrol.core.settings.AndroidAppSettings
import com.geepscontrol.ui.WeatherScreen
import com.geepscontrol.ui.WeatherTheme
import com.geepscontrol.ui.WeatherUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WeatherActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: AndroidWeatherRepository
    private var state by mutableStateOf(WeatherUiState())
    private var request: Job? = null
    private val permission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) refresh() else state = state.copy(message = "Location permission was not granted. You can search for a city or change app permissions below.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AndroidWeatherRepository(applicationContext)
        state = state.copy(enabled = repository.enabled, fahrenheit = repository.fahrenheit, afterAlarm = repository.afterAlarm)
        request =
            scope.launch {
                val cached = repository.cached()
                if (repository.enabled) {
                    state = state.copy(forecast = cached, cached = cached != null)
                    loadForecast()
                }
            }
        setContent {
            val theme = AndroidAppSettings(this).themeMode
            WeatherTheme(theme) {
                WeatherScreen(
                    state = state,
                    fromAlarm = intent.getBooleanExtra(EXTRA_FROM_ALARM, false),
                    onCurrentLocation = {
                        request?.cancel()
                        repository.select(null)
                        state = state.copy(enabled = true, forecast = null, places = emptyList(), message = null)
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            refresh()
                        } else {
                            permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    },
                    onRefresh = { refresh() },
                    onSearch = ::search,
                    onSelectPlace = { place ->
                        repository.select(place)
                        state = state.copy(enabled = true, forecast = null, places = emptyList())
                        refresh()
                    },
                    onUnits = {
                        repository.fahrenheit = it
                        state = state.copy(fahrenheit = it)
                    },
                    onAfterAlarm = {
                        repository.afterAlarm = it
                        state = state.copy(afterAlarm = it)
                    },
                    onTurnOff = {
                        request?.cancel()
                        repository.turnOff()
                        state = state.copy(enabled = false, afterAlarm = false, forecast = null, places = emptyList(), message = null, loading = false, searching = false)
                    },
                    onLocationSettings = { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                    onPermissionSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) },
                    onContinue = {
                        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_TODAY, true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                        finish()
                    },
                    onAttribution = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open-meteo.com/"))) },
                    formatTime = ::formatTime,
                )
            }
        }
    }

    private fun refresh() {
        request?.cancel()
        request = scope.launch { loadForecast() }
    }

    private suspend fun loadForecast() {
        state = state.copy(loading = true, searching = false, message = null, places = emptyList())
        try {
            val place = repository.selectedPlace() ?: repository.currentPlace()
            val forecast = repository.forecast(place)
            state = state.copy(forecast = forecast, cached = false, loading = false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            state = state.copy(loading = false, message = failure.weatherMessage(), cached = state.forecast != null)
        }
    }

    private fun search(query: String) {
        request?.cancel()
        request =
            scope.launch {
                state = state.copy(searching = true, loading = false, message = null, places = emptyList())
                try {
                    val places = repository.search(query)
                    state = state.copy(searching = false, places = places, message = if (places.isEmpty()) "No matching cities. Try a nearby city or include the country." else null)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    state = state.copy(searching = false, message = failure.weatherMessage())
                }
            }
    }

    override fun onStop() {
        request?.cancel()
        state = state.copy(loading = false, searching = false)
        super.onStop()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun Exception.weatherMessage(): String = if (this is IllegalStateException || this is IllegalArgumentException || message?.startsWith("Weather ") == true) message ?: "Weather is unavailable" else "Couldn't load weather. Check your connection and try Refresh, or choose a city."

    private fun formatTime(
        at: Long,
        timezone: String,
        includeDate: Boolean,
    ): String {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        val clock =
            if (android.text.format.DateFormat
                    .is24HourFormat(this)
            ) {
                "HH:mm"
            } else {
                "h:mm a"
            }
        val pattern = if (includeDate) "EEE, MMM d · $clock" else clock
        return DateTimeFormatter.ofPattern(pattern).format(Instant.ofEpochMilli(at).atZone(zone))
    }

    companion object {
        const val EXTRA_FROM_ALARM = "from_alarm"
    }
}
