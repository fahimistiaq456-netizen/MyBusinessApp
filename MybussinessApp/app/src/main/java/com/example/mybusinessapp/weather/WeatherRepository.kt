package com.example.mybusinessapp.weather

import android.content.Context
import com.example.mybusinessapp.weather.db.AppDatabase
import com.example.mybusinessapp.weather.db.WeatherDao
import com.example.mybusinessapp.weather.db.WeatherEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val apiService: WeatherApiService,
    private val weatherDao: WeatherDao? = null
) {

    suspend fun getWeather(lat: Double, lon: Double): Pair<WeatherResponse?, Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val networkResponse = apiService.getWeatherByLocation(lat, lon)
                // Save to Room cache
                saveToCache(networkResponse)
                Pair(networkResponse, false)
            } catch (e: Exception) {
                e.printStackTrace()
                // Offline fallback from Room database
                val cached = weatherDao?.getCachedWeatherDirect()
                if (cached != null) {
                    Pair(cached.toWeatherResponse(), true)
                } else {
                    Pair(null, false)
                }
            }
        }
    }

    suspend fun getWeatherByCity(city: String): Pair<WeatherResponse?, Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val networkResponse = apiService.getWeatherByCity(city)
                saveToCache(networkResponse)
                Pair(networkResponse, false)
            } catch (e: Exception) {
                e.printStackTrace()
                val cached = weatherDao?.getCachedWeatherDirect()
                if (cached != null) {
                    Pair(cached.toWeatherResponse(), true)
                } else {
                    Pair(null, false)
                }
            }
        }
    }

    suspend fun getCachedWeatherDirect(): WeatherEntity? {
        return withContext(Dispatchers.IO) {
            weatherDao?.getCachedWeatherDirect()
        }
    }

    private suspend fun saveToCache(response: WeatherResponse) {
        weatherDao?.let { dao ->
            val entity = WeatherEntity(
                id = 1,
                cityName = response.cityName.ifBlank { "Current Location" },
                temp = response.main.temp,
                feelsLike = response.main.feelsLike,
                humidity = response.main.humidity,
                pressure = response.main.pressure,
                windSpeed = response.wind.speed,
                condition = response.weather.firstOrNull()?.main ?: "Clear",
                description = response.weather.firstOrNull()?.description ?: "Clear Sky",
                uvi = response.uvi ?: 4.0,
                updatedAtTimestamp = System.currentTimeMillis()
            )
            dao.insertWeather(entity)
        }
    }

    private fun WeatherEntity.toWeatherResponse(): WeatherResponse {
        return WeatherResponse(
            main = Main(
                temp = this.temp,
                feelsLike = this.feelsLike,
                humidity = this.humidity,
                pressure = this.pressure
            ),
            weather = listOf(
                Weather(
                    main = this.condition,
                    description = this.description,
                    icon = "01d"
                )
            ),
            wind = Wind(speed = this.windSpeed),
            cityName = this.cityName,
            visibility = 10000,
            uvi = this.uvi
        )
    }

    companion object {
        fun create(context: Context): WeatherRepository {
            val api = WeatherApiService.create()
            val db = AppDatabase.getInstance(context)
            return WeatherRepository(api, db.weatherDao())
        }
    }
}
