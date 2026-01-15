package io.github.supercoderguy.lucentlauncher.data

import android.util.Log
import io.github.supercoderguy.lucentlauncher.model.WeatherData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class GeocodingResponse(val results: List<GeocodingResult>? = null)

@Serializable
private data class GeocodingResult(val latitude: Double, val longitude: Double)

@Serializable
private data class WeatherResponse(val current_weather: CurrentWeather)

@Serializable
private data class CurrentWeather(val temperature: Float, val weathercode: Int)

class WeatherRepository {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
                coerceInputValues = true
            })
        }
    }

    suspend fun fetchWeather(location: String): WeatherData? {
        return try {
            Log.d("LucentWeather", "Fetching coordinates for: $location")
            // 1. Get Coordinates
            val geoResponse: GeocodingResponse = client.get("https://geocoding-api.open-meteo.com/v1/search") {
                parameter("name", location)
                parameter("count", 1)
                parameter("language", "en")
                parameter("format", "json")
            }.body()

            val coords = geoResponse.results?.firstOrNull()
            if (coords == null) {
                Log.e("LucentWeather", "No coordinates found for location: $location")
                return null
            }

            Log.d("LucentWeather", "Found coordinates: ${coords.latitude}, ${coords.longitude}")

            // 2. Get Weather
            val weatherResponse: WeatherResponse = client.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", coords.latitude)
                parameter("longitude", coords.longitude)
                parameter("current_weather", true)
            }.body()

            Log.d("LucentWeather", "Weather fetched successfully: ${weatherResponse.current_weather.temperature}")

            WeatherData(
                temperature = weatherResponse.current_weather.temperature,
                condition = getWeatherCondition(weatherResponse.current_weather.weathercode)
            )
        } catch (e: Exception) {
            Log.e("LucentWeather", "Error fetching weather", e)
            null
        }
    }

    private fun getWeatherCondition(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1, 2, 3 -> "Mainly clear"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Rain showers"
            95 -> "Thunderstorm"
            else -> "Unknown ($code)"
        }
    }
}
