package com.example.mybusinessapp.weather

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(
        val weatherData: WeatherResponse,
        val isOfflineCache: Boolean = false,
        val cachedTimestamp: Long? = null
    ) : WeatherState()
    data class Error(val message: String) : WeatherState()
}
