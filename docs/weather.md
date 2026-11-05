# Weather today and alarm acknowledgment

## Behavior

The ringing alarm screen has a prominent Acknowledge button that stops sound and vibration immediately and completes the active occurrence. This remains a one-tap action even when a dismissal challenge is configured; Use dismissal challenge is available separately. Snoozing does not open weather. Timers do not open weather.

After acknowledgment, Weather today opens once any other queued alarms have been handled. The ringing notification's Acknowledge action follows the same path. Weather today is also available from the app's Menu. Continue to Today is always accessible, including while weather is loading or unavailable. Turning off Show weather after acknowledging alarms restores the direct acknowledgment-to-Today path.

The first weather screen explains the provider and location sharing before requesting either. Use current location asks for approximate foreground permission. City/postal-code search works without device location permission and shows up to five distinct location matches. An enabled weather screen refreshes on opening and supports manual refresh. It shows:

- Current temperature, conditions, and feels-like temperature
- Today's high/low and maximum rain probability
- Humidity, wind, maximum UV, sunrise, and sunset
- The current/upcoming twelve hourly forecast entries and a seven-day outlook
- Celsius/Fahrenheit selection, matching wind units, forecast-local dates/times, and the phone's 12/24-hour clock preference
- Download and forecast timestamps, clearly labeled saved data if refreshing fails

Denied permission, disabled/unavailable location, network failure, rate limits, absent optional forecast values, and empty city search results have recovery controls. Turning weather off clears the saved forecast and selected city and disables automatic after-alarm opening. No network or location operation is required to silence an alarm.

## Provider and data flow

The implementation uses [Open-Meteo's forecast API](https://open-meteo.com/en/docs) and [geocoding API](https://open-meteo.com/en/docs/geocoding-api) over HTTPS. No API key, account, SDK, analytics service, or app-owned backend is needed for the personal/non-commercial endpoint. Weather attribution is shown in the screen; location search credits GeoNames. The [provider's terms](https://open-meteo.com/en/terms) limit the free endpoint to non-commercial use and require attribution. Revisit those terms before commercial distribution rather than embedding a paid key in an APK.

Only rounded coordinates (two decimal places) or explicit city-search text are transmitted, along with ordinary HTTP connection information such as public IP. Organizer records and alarm labels are never included. Location acquisition is one foreground request with a fifteen-second limit, canceled if the screen closes. There are no fine/background-location permissions. Network requests have timeouts, bounded response sizes, and cancellation that disconnects the active request.

The latest response and display label are stored through AtomicFile in Android's no-backup directory. Cache writes are synchronized with clearing weather to prevent an in-flight request from recreating the cache after weather is disabled. Preferences store the chosen city, enablement, units, and after-alarm choice. See [PRIVACY.md](../PRIVACY.md).

## Implementation

- `AlarmActivity` owns immediate stop and the after-queue handoff. Notifications use an activity PendingIntent for acknowledgment so Android can legitimately open the follow-up screen.
- `WeatherActivity` owns permission requests, cancellable foreground work, and settings.
- `AndroidWeatherRepository` owns location acquisition, HTTP, preferences, and private cache.
- Shared `WeatherForecastParser` and `WeatherPresentation` handle forecast/geocoding parsing and presentation rules; `WeatherScreen` uses the app theme and scrollable accessible text.
- No Room schema changes are needed for weather.

## Verification

Shared tests cover forecast parsing, optional/missing values, unit conversion, and ambiguous/invalid geocoding results. Android flow tests press the real native Acknowledge button with a Number challenge configured and exercise acknowledgment from a real alarm notification; both must persist completion and open Weather today. The ordinary alarm regression suite continues to cover skip, snooze, stale actions, and recurrence.

An explicit opt-in `WeatherLiveSmokeTest` checks the real service and renders the actual screen using public Tokyo coordinates. It is skipped during ordinary offline tests and never reads the device's location. Run it with the Android instrumentation argument `weather_live=true`.

Physical-device acceptance still includes coarse-location behavior across manufacturers, locked-screen handoff/unlock, offline recovery, TalkBack, large fonts, and repeated alarms. Forecasts are model data and may be delayed or unavailable; this feature does not provide official severe-weather alerts.

Current verification: debug APK assembly and Android lint pass (existing warnings remain); all 68 shared/JVM tests and eight offline Android alarm/acknowledgment-flow tests pass. The live Android smoke test also passes: the Open-Meteo endpoint returns seven daily forecasts using public Tokyo coordinates, the forecast cache round-trips, and the Weather today screen renders for visual inspection. Physical-device location, lock-screen, and accessibility checks remain on the acceptance list.
