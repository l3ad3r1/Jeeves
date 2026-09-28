package com.hermes.agent.data.weather

import android.content.Context
import com.hermes.agent.data.plugins.DeviceWeather
import com.hermes.agent.data.plugins.DeviceWeatherReading
import com.sassybutler.alarm.WeatherService

/**
 * The Weather plugin's "here", served by Daybook: the same location, source and
 * cache as the Daybook weather strip, so the agent and Daybook never disagree.
 * A fresh fetch also refreshes Daybook's cache; offline, Daybook's cache is used.
 */
class DaybookDeviceWeather(private val context: Context) : DeviceWeather {
    override suspend fun current(): DeviceWeatherReading? {
        val weather = WeatherService.refresh(context) ?: WeatherService.cached(context) ?: return null
        return DeviceWeatherReading(
            tempC = weather.tempC,
            conditions = weather.label.lowercase(),
            source = "Daybook, device location",
            fetchedAt = weather.fetchedAt,
        )
    }
}
