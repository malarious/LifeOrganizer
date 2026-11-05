# Life Minder Privacy

Life Minder is a local-first application. Organizer features do not require an account, internet connection, remote server, analytics service, advertising service, or API key. The optional Weather today screen uses internet access; the manifest now requests Internet and approximate foreground-location permissions for that feature.

Projects, tasks, timers, alarms, work sessions, settings, and reviews remain on the device unless the user explicitly exports or backs them up. Exports and backups are written to Life Minder's application documents directory and remain under the user's control.

Tasks, calendar entries, medications, trips, alarm labels, and other organizer records are never sent to the weather provider.

## Optional weather

- Acknowledging a clock alarm can open Weather today. Merely opening that screen does not send your location or start a forecast request until you choose a city or choose Use current location. Once enabled, weather refreshes when this screen opens. Turn off Show weather after acknowledging alarms to skip the automatic screen.
- Use current location asks Android for approximate location access while the screen is open. No precise or background-location permission is requested, and there is no continuous tracking. Device coordinates are rounded to two decimal places before being sent over HTTPS to Open-Meteo.
- City search sends the search text to Open-Meteo's geocoding service. Choosing a city uses that city's approximate coordinates without requiring device location permission. As with any internet request, the provider can see the device's public IP address.
- The latest forecast is cached in app-private storage excluded from Android cloud backup. A chosen city and weather preferences are stored locally. A device location fix is used in memory; the provider's returned forecast grid location may be present in the cached forecast.
- Requests are initiated only from the foreground weather screen, and active location/network requests are canceled when it is closed. Weather failures never prevent acknowledgment or ordinary offline organizer use.
- Turn weather off and clear saved forecast removes the weather cache and selected city, disables weather fetching, and disables the after-alarm screen. The feature remains accessible through Menu → Weather today if you choose to enable it again.
- Weather data is provided by [Open-Meteo](https://open-meteo.com/) under CC BY 4.0; location search is based on GeoNames. Review the provider's [terms and privacy information](https://open-meteo.com/en/terms). The key-free endpoint is for non-commercial use; commercial distribution requires checking the provider's current terms.

Uninstalling the application may remove app-private data unless Android backup or a user-created Life Minder export/backup preserves it.
