package com.example.mybusinessapp.weather

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(val weatherData: WeatherResponse) : WeatherState()
    data class Error(val message: String) : WeatherState()
}
