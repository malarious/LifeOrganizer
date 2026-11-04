package com.geepscontrol.app

import android.content.Intent
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.geepscontrol.core.domain.WeatherPlace
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Explicit opt-in smoke test; uses public Tokyo coordinates, never the device's location. */
class WeatherLiveSmokeTest {
    @Test fun publicCityForecastLoadsAndRendersWhenRequested() =
        runBlocking {
            assumeTrue(InstrumentationRegistry.getArguments().getString("weather_live") == "true")
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            val repository = AndroidWeatherRepository(context)
            val originalPlace = repository.selectedPlace()
            val originalEnabled = repository.enabled
            val originalUnits = repository.fahrenheit
            val originalAfter = repository.afterAlarm
            val cache = File(context.noBackupFilesDir, "weather.json")
            val originalCache = cache.takeIf { it.exists() }?.readBytes()
            var activity: WeatherActivity? = null
            try {
                val place = WeatherPlace(35.68, 139.69, "Tokyo, Japan")
                repository.select(place)
                repository.fahrenheit = false
                val forecast = repository.forecast(place)
                assertEquals(7, forecast.days.size)
                assertTrue(forecast.hours.isNotEmpty())
                assertEquals("Asia/Tokyo", forecast.timezone)
                activity = instrumentation.startActivitySync(Intent(context, WeatherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as WeatherActivity
                assertEquals(forecast.place, repository.cached()!!.place)
            instrumentation.waitForIdleSync()
            // Allow the first Compose frame to reach the screenshot surface; inspect the saved image.
            android.os.SystemClock.sleep(800)
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
                File(context.getExternalFilesDir(null), "weather-preview.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                screenshot.recycle()
            } finally {
                instrumentation.runOnMainSync { activity?.finish() }
                repository.select(originalPlace)
                repository.enabled = originalEnabled
                repository.fahrenheit = originalUnits
                repository.afterAlarm = originalAfter
                if (originalCache == null) cache.delete() else cache.writeBytes(originalCache)
            }
        }
}
