package com.sassybutler.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * WeatherService — geo-located current weather via Open-Meteo
 * (https://open-meteo.com — open-source, no API key required).
 *
 * Strategy: the Parlour refreshes on every open; results are cached in
 * SharedPreferences so the alarm path (which may fire in Doze with no
 * network) never blocks — [cached] serves anything younger than
 * [STALE_AFTER_MS] and the greeting simply omits weather when the cache
 * is too old or location/permission is unavailable.
 */
object WeatherService {

    data class Weather(val tempC: Int, val code: Int, val fetchedAt: Long) {
        /** Short display label, e.g. "Clear" for the Parlour strip. */
        val label: String get() = when (code) {
            0            -> "Clear"
            1, 2         -> "Partly cloudy"
            3            -> "Overcast"
            45, 48       -> "Fog"
            in 51..57    -> "Drizzle"
            in 61..67, in 80..82 -> "Rain"
            in 71..77, 85, 86    -> "Snow"
            in 95..99    -> "Storm"
            else         -> "—"
        }

        /** Butler-spoken condition; every word is in the TTS lexicon. */
        val spoken: String get() = when (code) {
            0            -> "clear skies"
            1, 2         -> "a few clouds"
            3            -> "a grey and dismal sky"
            45, 48       -> "fog"
            in 51..57    -> "drizzle"
            in 61..67, in 80..82 -> "rain"
            in 71..77, 85, 86    -> "snow"
            in 95..99    -> "a thunderstorm"
            else         -> ""
        }

        /** "It is sixteen degrees, with a grey and dismal sky." */
        fun sentence(): String {
            val temp = if (tempC < 0) "minus ${-tempC}" else "$tempC"
            val condition = spoken
            return if (condition.isEmpty()) "It is $temp degrees."
            else "It is $temp degrees, with $condition."
        }
    }

    private const val TAG = "WeatherService"
    private const val PREFS = "weather_cache"
    private const val STALE_AFTER_MS = 12 * 60 * 60 * 1000L // 12 h

    /** Cached weather, or null if absent/older than [maxAgeMs]. */
    fun cached(context: Context, maxAgeMs: Long = STALE_AFTER_MS): Weather? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fetchedAt = p.getLong("fetched_at", 0L)
        if (fetchedAt == 0L || System.currentTimeMillis() - fetchedAt > maxAgeMs) return null
        return Weather(p.getInt("temp_c", 0), p.getInt("code", -1), fetchedAt)
    }

    /**
     * Fetch fresh weather for the device's last known location and cache it.
     * Returns null (leaving any previous cache intact) when location
     * permission is missing, no fix is available, or the network fails.
     */
    suspend fun refresh(context: Context): Weather? = withContext(Dispatchers.IO) {
        val location = lastKnownLocation(context)
            ?: requestSingleLocation(context)
            ?: run {
                Log.d(TAG, "No location available — weather skipped")
                return@withContext null
            }

        try {
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code",
                location.latitude, location.longitude,
            )
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val current = JSONObject(body).getJSONObject("current")
            val weather = Weather(
                tempC = Math.round(current.getDouble("temperature_2m")).toInt(),
                code = current.getInt("weather_code"),
                fetchedAt = System.currentTimeMillis(),
            )

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("temp_c", weather.tempC)
                .putInt("code", weather.code)
                .putLong("fetched_at", weather.fetchedAt)
                .apply()

            Log.i(TAG, "Weather: ${weather.tempC}°C, ${weather.label}")
            weather
        } catch (e: Exception) {
            Log.w(TAG, "Weather fetch failed", e)
            null
        }
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasLocationPermission
    private fun lastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return providers(lm).firstNotNullOfOrNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }
    }

    /**
     * Active one-shot fix for when last-known is empty (fresh installs,
     * fresh emulators). Coarse accuracy is plenty for city-level weather.
     */
    @SuppressLint("MissingPermission")
    private suspend fun requestSingleLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Try each enabled provider in turn — fused/network may have no data
        // source (fresh device, emulator) while GPS delivers fine.
        for (provider in providers(lm)) {
            if (!runCatching { lm.isProviderEnabled(provider) }.getOrDefault(false)) continue
            val location = withTimeoutOrNull(6_000) {
                suspendCancellableCoroutine { cont ->
                    val delivered = AtomicBoolean(false)
                    fun deliver(loc: Location?) {
                        if (delivered.compareAndSet(false, true)) cont.resume(loc)
                    }
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            lm.getCurrentLocation(provider, null, context.mainExecutor) { deliver(it) }
                        } else {
                            @Suppress("DEPRECATION")
                            lm.requestSingleUpdate(provider, { deliver(it) }, Looper.getMainLooper())
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Active $provider request failed", e)
                        deliver(null)
                    }
                }
            }
            if (location != null) {
                Log.d(TAG, "Got location from active $provider request")
                return location
            }
        }
        return null
    }

    /** Fused (31+, coarse-friendly) first, then network, then GPS. */
    private fun providers(lm: LocationManager): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.PASSIVE_PROVIDER)
    }.filter { it in lm.allProviders }
}
