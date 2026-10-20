package com.example.mybusinessapp.ui.weather

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybusinessapp.ui.FluidGlassCard
import com.example.mybusinessapp.ui.Soft3DIcon
import com.example.mybusinessapp.weather.WeatherState
import com.example.mybusinessapp.weather.WeatherViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SimpleWeatherWidget(weatherViewModel: WeatherViewModel = viewModel()) {
    val weatherState by weatherViewModel.weatherState.collectAsState()
    val currentTime by weatherViewModel.currentTime.collectAsState()
    val currentDay by weatherViewModel.currentDay.collectAsState()

    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    fun requestLocationUpdate() {
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    weatherViewModel.fetchWeatherByLocation(location.latitude, location.longitude)
                }
            }
        } catch (e: SecurityException) { }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            requestLocationUpdate()
        } else {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    FluidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        onClick = {
            if (locationPermissionsState.allPermissionsGranted) {
                requestLocationUpdate()
            } else {
                locationPermissionsState.launchMultiplePermissionRequest()
            }
        }
    ) {
        when (val state = weatherState) {
            is WeatherState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Detecting Location...", color = colors.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
            is WeatherState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Warning, null, tint = colors.error, modifier = Modifier.size(40.dp))
                        Text("GPS Error", color = colors.onSurface, fontWeight = FontWeight.Bold)
                        Text(state.message, color = colors.onSurfaceVariant, fontSize = 12.sp)
                        Button(onClick = { requestLocationUpdate() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Retry GPS")
                        }
                    }
                }
            }
            is WeatherState.Success -> {
                val weatherData = state.weatherData
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = currentTime,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.onSurface,
                                letterSpacing = (-2).sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF00E676), CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "LIVE • $currentDay",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676),
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${weatherData.main.temp.toInt()}°",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Light,
                                    color = colors.tertiary
                                )
                                Spacer(Modifier.width(8.dp))
                                WeatherIcon3D(condition = weatherData.weather.firstOrNull()?.main ?: "Clear")
                            }
                            Text(
                                text = "Feels like ${weatherData.main.feelsLike.toInt()}°",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = weatherData.cityName.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // BENTO STYLE STAT GRID
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WeatherDetailItem(
                            label = "Humidity",
                            value = "${weatherData.main.humidity}%",
                            icon = Icons.Rounded.WaterDrop,
                            color = Color(0xFF42A5F5)
                        )
                        WeatherDetailItem(
                            label = "Wind",
                            value = "${weatherData.wind.speed}m/s",
                            icon = Icons.Rounded.Air,
                            color = Color(0xFF26A69A)
                        )
                        WeatherDetailItem(
                            label = "UV Index",
                            value = "${weatherData.uvi?.toInt() ?: 0}",
                            icon = Icons.Rounded.WbSunny,
                            color = Color(0xFFFFB300)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WeatherDetailItem(
                            label = "Visibility",
                            value = "${(weatherData.visibility / 1000)}km",
                            icon = Icons.Rounded.Visibility,
                            color = Color(0xFFFF7043)
                        )
                        WeatherDetailItem(
                            label = "Pressure",
                            value = "${weatherData.main.pressure}",
                            icon = Icons.Rounded.Speed,
                            color = Color(0xFFAB47BC)
                        )
                        WeatherDetailItem(
                            label = "Condition",
                            value = weatherData.weather.firstOrNull()?.main ?: "Clear",
                            icon = Icons.Rounded.CloudQueue,
                            color = Color(0xFF90A4AE)
                        )
                    }

                    if (weatherData.sys != null) {
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Soft3DIcon(Icons.Rounded.WbTwilight, Color(0xFFFFB74D), size = 32.dp, iconSize = 16.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Sunrise: ${weatherViewModel.formatTime(weatherData.sys.sunrise)}", fontSize = 12.sp, color = colors.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Soft3DIcon(Icons.Rounded.WbTwilight, Color(0xFFFB8C00), size = 32.dp, iconSize = 16.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Sunset: ${weatherViewModel.formatTime(weatherData.sys.sunset)}", fontSize = 12.sp, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun WeatherIcon3D(condition: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "rotation"
    )

    val (icon, color) = when (condition.lowercase()) {
        "clear" -> Icons.Rounded.WbSunny to Color(0xFFFFD600)
        "clouds" -> Icons.Rounded.Cloud to Color(0xFFB0BEC5)
        "rain" -> Icons.Rounded.Umbrella to Color(0xFF64B5F6)
        else -> Icons.Rounded.WbSunny to Color(0xFFFFD600)
    }

    Soft3DIcon(
        imageVector = icon,
        color = color,
        size = 56.dp,
        iconSize = 28.dp,
        modifier = Modifier.graphicsLayer { rotationZ = rotation }
    )
}

@Composable
fun WeatherDetailItem(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Soft3DIcon(
            imageVector = icon,
            color = color,
            size = 42.dp,
            iconSize = 20.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
    }
}
