package com.geepscontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.geepscontrol.core.domain.WeatherForecast
import com.geepscontrol.core.domain.WeatherPlace
import com.geepscontrol.core.domain.WeatherPresentation
import com.geepscontrol.core.settings.ThemeMode
import kotlin.math.roundToInt

data class WeatherUiState(
    val forecast: WeatherForecast? = null,
    val loading: Boolean = false,
    val searching: Boolean = false,
    val message: String? = null,
    val places: List<WeatherPlace> = emptyList(),
    val enabled: Boolean = false,
    val cached: Boolean = false,
    val fahrenheit: Boolean = false,
    val afterAlarm: Boolean = true,
)

@Composable
fun WeatherTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit,
) {
    GeepsTheme(
        darkTheme =
            when (mode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            },
        content = content,
    )
}

@Composable
fun WeatherScreen(
    state: WeatherUiState,
    fromAlarm: Boolean,
    onCurrentLocation: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onSelectPlace: (WeatherPlace) -> Unit,
    onUnits: (Boolean) -> Unit,
    onAfterAlarm: (Boolean) -> Unit,
    onTurnOff: () -> Unit,
    onLocationSettings: () -> Unit,
    onPermissionSettings: () -> Unit,
    onContinue: () -> Unit,
    onAttribution: () -> Unit,
    formatTime: (Long, String, Boolean) -> String,
) {
    var city by rememberSaveable { mutableStateOf("") }

    fun temp(value: Double?) = WeatherPresentation.temperature(value, state.fahrenheit)

    fun percent(value: Double?) = value?.let { "${it.roundToInt()}%" } ?: "—"
    Scaffold(bottomBar = {
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) { Text("Continue to Today") }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (fromAlarm) Text("Alarm acknowledged", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Weather today", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("A quick look at the day ahead", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!state.enabled) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Your forecast, when you want it", style = MaterialTheme.typography.titleLarge)
                            Text("Use your approximate location or search for a city. Open-Meteo receives the approximate coordinates to provide weather; city searches send the text you enter. Weather uses internet access only while this screen is open. Your organizer data stays on this device.")
                            Text("Choosing a location enables weather when you open this screen again. You can turn it off below.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            state.message?.let { message ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(message)
                            if (state.forecast != null) Text("Showing a saved forecast. Conditions may have changed.", fontWeight = FontWeight.SemiBold)
                            if (state.enabled) TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Retry weather") }
                        }
                    }
                }
            }
            if (state.loading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        Text("Updating your weather…")
                    }
                }
            }
            state.forecast?.let { forecast ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(forecast.place, style = MaterialTheme.typography.titleMedium)
                            Text(temp(forecast.temperature), style = MaterialTheme.typography.displayLarge)
                            Text(WeatherPresentation.description(forecast.code), style = MaterialTheme.typography.headlineSmall)
                            Text("Feels like ${temp(forecast.feelsLike)}")
                            forecast.days.firstOrNull()?.let { today -> Text("High ${temp(today.high)} · Low ${temp(today.low)} · Rain ${percent(today.rainChance)}") }
                            Text("${if (state.cached) "Saved forecast" else "Updated"}: ${formatTime(forecast.fetchedAt, forecast.timezone, true)}", style = MaterialTheme.typography.bodySmall)
                            Text("Local forecast time: ${formatTime(forecast.currentAt, forecast.timezone, true)}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onUnits(false) }, enabled = state.fahrenheit) { Text("°C") }
                                OutlinedButton(onClick = { onUnits(true) }, enabled = !state.fahrenheit) { Text("°F") }
                                TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Refresh") }
                            }
                        }
                    }
                }
                item {
                    val today = forecast.days.firstOrNull()
                    val wind = forecast.windKmh?.let { "${(if (state.fahrenheit) it / 1.609344 else it).roundToInt()} ${if (state.fahrenheit) "mph" else "km/h"}" } ?: "—"
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (state.cached) "Saved forecast details" else "Today's details", style = MaterialTheme.typography.titleLarge)
                        Text("Humidity ${percent(forecast.humidity)} · Wind $wind")
                        Text("Maximum UV ${today?.uv?.let { ((it * 10).roundToInt() / 10.0).toString() } ?: "—"}")
                        Text("Sunrise ${today?.sunrise?.let { formatTime(it, forecast.timezone, false) } ?: "—"} · Sunset ${today?.sunset?.let { formatTime(it, forecast.timezone, false) } ?: "—"}")
                    }
                }
                if (forecast.hours.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Next hours", style = MaterialTheme.typography.titleLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(forecast.hours) { hour ->
                                    Card(Modifier.width(144.dp)) {
                                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(formatTime(hour.at, forecast.timezone, false), fontWeight = FontWeight.Bold)
                                            Text(temp(hour.temperature), style = MaterialTheme.typography.titleLarge)
                                            Text(WeatherPresentation.description(hour.code), style = MaterialTheme.typography.bodySmall)
                                            Text("Rain ${percent(hour.rainChance)}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (forecast.days.isNotEmpty()) item { Text("Seven-day outlook", style = MaterialTheme.typography.titleLarge) }
                items(forecast.days) { day ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(formatTime(day.at, forecast.timezone, true).substringBefore(" · "), fontWeight = FontWeight.Bold)
                            Text(WeatherPresentation.description(day.code))
                            Text("High ${temp(day.high)} · Low ${temp(day.low)} · Rain ${percent(day.rainChance)}")
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Forecast location", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onCurrentLocation, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text("Use current location") }
                    OutlinedTextField(city, { city = it }, label = { Text("City or postal code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { onSearch(city) }, enabled = city.trim().length >= 2 && !state.searching, modifier = Modifier.fillMaxWidth()) { Text(if (state.searching) "Searching…" else "Search cities") }
                    Text("City search sends your search text to Open-Meteo. No device location permission is needed.", style = MaterialTheme.typography.bodySmall)
                }
            }
            items(state.places) { place -> OutlinedButton(onClick = { onSelectPlace(place) }, modifier = Modifier.fillMaxWidth()) { Text(place.name) } }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onLocationSettings) { Text("Android location settings") }
                    TextButton(onClick = onPermissionSettings) { Text("App permissions") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(state.afterAlarm, onAfterAlarm)
                        Text("Show weather after acknowledging alarms", Modifier.padding(start = 8.dp).weight(1f))
                    }
                    if (state.enabled) TextButton(onClick = onTurnOff) { Text("Turn weather off and clear saved forecast") }
                    TextButton(onClick = onAttribution) { Text("Weather data by Open-Meteo · CC BY 4.0") }
                    Text("Location search: GeoNames via Open-Meteo", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
