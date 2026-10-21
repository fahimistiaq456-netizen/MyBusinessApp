package com.example.mybusinessapp.weather.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherEntity(
    @PrimaryKey
    val id: Int = 1, // Single cached weather record for offline fallback
    val cityName: String,
    val temp: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val condition: String,
    val description: String,
    val uvi: Double? = 0.0,
    val updatedAtTimestamp: Long = System.currentTimeMillis()
)
