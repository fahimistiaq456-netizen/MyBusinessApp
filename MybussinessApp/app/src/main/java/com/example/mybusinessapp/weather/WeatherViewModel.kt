package com.example.mybusinessapp.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository(WeatherApiService.create())
) : ViewModel() {

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> get() = _weatherState

    // Live-updating clock
    private val ticker = flow {
        while (true) {
            emit(Date())
            delay(60_000)
        }
    }

    val currentTime: StateFlow<String> = ticker.map { getFormattedTime(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), getFormattedTime(Date()))

    val currentDate: StateFlow<String> = ticker.map { getFormattedDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), getFormattedDate(Date()))

    val currentDay: StateFlow<String> = ticker.map { getFormattedDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), getFormattedDay(Date()))

    private var lastCity: String? = null
    private var lastCoords: Pair<Double, Double>? = null

    init {
        startPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(600_000) // Refresh every 10 minutes
                refreshWeather()
            }
        }
    }

    private fun refreshWeather() {
        lastCoords?.let { (lat, lon) ->
            fetchWeatherByLocation(lat, lon)
        } ?: lastCity?.let {
            fetchWeather(it)
        }
    }

    fun fetchWeather(city: String) {
        lastCity = city
        lastCoords = null
        _weatherState.value = WeatherState.Loading
        viewModelScope.launch {
            val response = repository.getWeatherByCity(city)
            _weatherState.value = if (response != null) {
                WeatherState.Success(response)
            } else {
                WeatherState.Error("Unable to fetch weather.")
            }
        }
    }

    fun fetchWeatherByLocation(lat: Double, lon: Double) {
        lastCoords = lat to lon
        lastCity = null
        _weatherState.value = WeatherState.Loading
        viewModelScope.launch {
            val response = repository.getWeather(lat, lon)
            _weatherState.value = if (response != null) {
                WeatherState.Success(response)
            } else {
                WeatherState.Error("Unable to fetch weather for your location.")
            }
        }
    }

    private fun getFormattedTime(date: Date): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    private fun getFormattedDate(date: Date): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    private fun getFormattedDay(date: Date): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(date)
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }
}
