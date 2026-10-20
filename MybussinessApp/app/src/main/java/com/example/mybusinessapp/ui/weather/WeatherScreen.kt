package com.example.mybusinessapp.ui.weather

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.mybusinessapp.weather.WeatherState
import com.example.mybusinessapp.weather.WeatherViewModel

@Composable
fun WeatherScreen(weatherViewModel: WeatherViewModel = viewModel()) {
    val weatherState by weatherViewModel.weatherState.collectAsState()
    val currentTime by weatherViewModel.currentTime.collectAsState()
    val currentDate by weatherViewModel.currentDate.collectAsState()
    val currentDay by weatherViewModel.currentDay.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = weatherState) {
            is WeatherState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is WeatherState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp
                    )
                }
            }
            is WeatherState.Success -> {
                val weatherData = state.weatherData
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = weatherData.cityName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val iconUrl = "https://openweathermap.org/img/wn/${weatherData.weather.firstOrNull()?.icon ?: "01d"}@2x.png"
                    Image(
                        painter = rememberAsyncImagePainter(iconUrl),
                        contentDescription = weatherData.weather.firstOrNull()?.description,
                        modifier = Modifier.size(100.dp),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = "${weatherData.main.temp.toInt()}°C",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Text(
                        text = weatherData.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "$currentDay, $currentDate",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentTime,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        WeatherInfoItem(label = "Humidity", value = "${weatherData.main.humidity}%")
                        WeatherInfoItem(label = "Pressure", value = "${weatherData.main.pressure} hPa")
                        WeatherInfoItem(label = "Wind", value = "${weatherData.wind.speed} m/s")
                    }

                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
        }
    }
}

@Composable
fun WeatherInfoItem(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.onSurface)
        Text(text = label, fontSize = 14.sp, color = colors.onSurfaceVariant)
    }
}
