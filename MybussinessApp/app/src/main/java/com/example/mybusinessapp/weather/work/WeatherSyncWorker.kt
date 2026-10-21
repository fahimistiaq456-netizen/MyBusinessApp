package com.example.mybusinessapp.weather.work

import android.content.Context
import androidx.work.*
import com.example.mybusinessapp.weather.WeatherApiService
import com.example.mybusinessapp.weather.db.AppDatabase
import com.example.mybusinessapp.weather.db.WeatherEntity
import java.util.concurrent.TimeUnit

// ==================================================================
// ☁️ WORKMANAGER PERIODIC WEATHER SYNC WORKER
// ==================================================================
class WeatherSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val apiService = WeatherApiService.create()
            // Default sync coordinate for business HQ or last known location (San Francisco: 37.7749, -122.4194)
            val response = apiService.getWeatherByLocation(37.7749, -122.4194)

            val entity = WeatherEntity(
                id = 1,
                cityName = response.cityName.ifBlank { "San Francisco" },
                temp = response.main.temp,
                feelsLike = response.main.feelsLike,
                humidity = response.main.humidity,
                pressure = response.main.pressure,
                windSpeed = response.wind.speed,
                condition = response.weather.firstOrNull()?.main ?: "Clear",
                description = response.weather.firstOrNull()?.description ?: "clear sky",
                uvi = response.uvi ?: 4.2,
                updatedAtTimestamp = System.currentTimeMillis()
            )

            val db = AppDatabase.getInstance(applicationContext)
            db.weatherDao().insertWeather(entity)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // If network fails, retry according to WorkManager backoff policy
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "WeatherSyncPeriodicWork"

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
                repeatInterval = 3,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
