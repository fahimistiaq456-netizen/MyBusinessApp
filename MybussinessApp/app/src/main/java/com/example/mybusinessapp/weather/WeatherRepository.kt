package com.example.mybusinessapp.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(private val apiService: WeatherApiService) {

    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getWeatherByLocation(lat, lon)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getWeatherByCity(city: String): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getWeatherByCity(city)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
