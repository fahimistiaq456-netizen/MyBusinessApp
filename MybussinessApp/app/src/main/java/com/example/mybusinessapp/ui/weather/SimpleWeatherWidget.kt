package com.example.mybusinessapp.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybusinessapp.ui.FluidGlassCard
import com.example.mybusinessapp.ui.Soft3DIcon
import com.example.mybusinessapp.weather.WeatherState
import com.example.mybusinessapp.weather.WeatherViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@Composable
fun SimpleWeatherWidget(weatherViewModel: WeatherViewModel = viewModel()) {
    val weatherState by weatherViewModel.weatherState.collectAsState()
    val currentTime by weatherViewModel.currentTime.collectAsState()
    val currentDay by weatherViewModel.currentDay.collectAsState()

    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    var permissionGranted by remember { mutableStateOf(hasLocationPermission()) }

    fun requestLocationUpdate() {
        if (!hasLocationPermission()) return
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    weatherViewModel.fetchWeatherByLocation(location.latitude, location.longitude)
                } else {
                    weatherViewModel.fetchWeather("San Francisco")
                }
            }.addOnFailureListener {
                weatherViewModel.fetchWeather("San Francisco")
            }
        } catch (e: SecurityException) {
            weatherViewModel.fetchWeather("San Francisco")
        }
    }

    // Modern runtime permission handling using rememberLauncherForActivityResult
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            permissionGranted = true
            requestLocationUpdate()
        } else {
            // Fallback to default city weather if user denied GPS
            weatherViewModel.fetchWeather("San Francisco")
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission()) {
            requestLocationUpdate()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    FluidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        onClick = {
            if (hasLocationPermission()) {
                requestLocationUpdate()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
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
                        Text("Detecting Location & Weather...", color = colors.onSurfaceVariant, fontSize = 12.sp)
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
                        Icon(Icons.Rounded.CloudOff, null, tint = colors.error, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Weather Unavailable", color = colors.onSurface, fontWeight = FontWeight.Bold)
                        Text(state.message, color = colors.onSurfaceVariant, fontSize = 12.sp)
                        Button(
                            onClick = {
                                if (hasLocationPermission()) requestLocationUpdate()
                                else weatherViewModel.fetchWeather("San Francisco")
                            },
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Text("Retry Weather")
                        }
                    }
                }
            }
            is WeatherState.Success -> {
                val weatherData = state.weatherData
                val isOffline = state.isOfflineCache

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

                            // Live vs Offline Cache indicator with relative timestamp
                            if (isOffline) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFB74D), CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    val relTime = state.cachedTimestamp?.let {
                                        weatherViewModel.getRelativeTimeSpan(it)
                                    } ?: "recently"
                                    Text(
                                        text = "OFFLINE • Updated $relTime",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB74D),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            } else {
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

                    // BENTO STYLE STAT GRID: Temp, Humidity, Wind speed, UV Index, Air Pressure
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
                            value = "${weatherData.uvi?.toInt() ?: 4}",
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
                            label = "Pressure",
                            value = "${weatherData.main.pressure} hPa",
                            icon = Icons.Rounded.Speed,
                            color = Color(0xFFAB47BC)
                        )
                        WeatherDetailItem(
                            label = "Visibility",
                            value = "${(weatherData.visibility / 1000)}km",
                            icon = Icons.Rounded.Visibility,
                            color = Color(0xFFFF7043)
                        )
                        WeatherDetailItem(
                            label = "Condition",
                            value = weatherData.weather.firstOrNull()?.main ?: "Clear",
                            icon = Icons.Rounded.CloudQueue,
                            color = Color(0xFF29B6F6)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WeatherIcon3D(condition: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "weatherAnim")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weatherFloat"
    )

    val (icon, color) = when (condition.lowercase()) {
        "clouds" -> Icons.Rounded.Cloud to Color(0xFF90CAF9)
        "rain" -> Icons.Rounded.WaterDrop to Color(0xFF42A5F5)
        "thunderstorm" -> Icons.Rounded.FlashOn to Color(0xFFFFD54F)
        "snow" -> Icons.Rounded.AcUnit to Color(0xFFE0E0E0)
        else -> Icons.Rounded.WbSunny to Color(0xFFFFB300)
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .graphicsLayer { translationY = floatOffset },
        contentAlignment = Alignment.Center
    ) {
        Soft3DIcon(imageVector = icon, color = color, size = 52.dp, iconSize = 28.dp)
    }
}
