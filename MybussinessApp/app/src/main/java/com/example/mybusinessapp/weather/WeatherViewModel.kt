package com.example.mybusinessapp.weather

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybusinessapp.weather.work.WeatherSyncWorker
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

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository.create(application)

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
        // Enqueue background sync via WorkManager (runs every 3 hours with NetworkType.CONNECTED)
        WeatherSyncWorker.enqueuePeriodicSync(application)

        // Load cached Room data immediately so offline users see data right away
        loadCachedDataOnLaunch()

        startPeriodicRefresh()
    }

    private fun loadCachedDataOnLaunch() {
        viewModelScope.launch {
            val cached = repository.getCachedWeatherDirect()
            if (cached != null && _weatherState.value is WeatherState.Loading) {
                val response = WeatherResponse(
                    main = Main(
                        temp = cached.temp,
                        feelsLike = cached.feelsLike,
                        humidity = cached.humidity,
                        pressure = cached.pressure
                    ),
                    weather = listOf(
                        Weather(
                            main = cached.condition,
                            description = cached.description,
                            icon = "01d"
                        )
                    ),
                    wind = Wind(speed = cached.windSpeed),
                    cityName = cached.cityName,
                    visibility = 10000,
                    uvi = cached.uvi
                )
                _weatherState.value = WeatherState.Success(
                    weatherData = response,
                    isOfflineCache = true,
                    cachedTimestamp = cached.updatedAtTimestamp
                )
            }
        }
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(600_000) // Refresh every 10 minutes when active
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
        viewModelScope.launch {
            val (response, isCache) = repository.getWeatherByCity(city)
            _weatherState.value = if (response != null) {
                val cached = repository.getCachedWeatherDirect()
                WeatherState.Success(
                    weatherData = response,
                    isOfflineCache = isCache,
                    cachedTimestamp = cached?.updatedAtTimestamp ?: System.currentTimeMillis()
                )
            } else {
                WeatherState.Error("Unable to fetch weather.")
            }
        }
    }

    fun fetchWeatherByLocation(lat: Double, lon: Double) {
        lastCoords = lat to lon
        lastCity = null
        viewModelScope.launch {
            val (response, isCache) = repository.getWeather(lat, lon)
            _weatherState.value = if (response != null) {
                val cached = repository.getCachedWeatherDirect()
                WeatherState.Success(
                    weatherData = response,
                    isOfflineCache = isCache,
                    cachedTimestamp = cached?.updatedAtTimestamp ?: System.currentTimeMillis()
                )
            } else {
                WeatherState.Error("Unable to fetch weather for your location.")
            }
        }
    }

    fun getRelativeTimeSpan(timestamp: Long): String {
        return try {
            DateUtils.getRelativeTimeSpanString(
                timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        } catch (e: Exception) {
            "recently"
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
