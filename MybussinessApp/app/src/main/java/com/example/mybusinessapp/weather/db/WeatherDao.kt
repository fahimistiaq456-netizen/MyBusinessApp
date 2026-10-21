package com.example.mybusinessapp.weather.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather_cache WHERE id = 1 LIMIT 1")
    fun getCachedWeather(): Flow<WeatherEntity?>

    @Query("SELECT * FROM weather_cache WHERE id = 1 LIMIT 1")
    suspend fun getCachedWeatherDirect(): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(entity: WeatherEntity)

    @Query("DELETE FROM weather_cache")
    suspend fun clearCache()
}
