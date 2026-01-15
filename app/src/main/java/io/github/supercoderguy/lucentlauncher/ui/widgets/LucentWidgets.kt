package io.github.supercoderguy.lucentlauncher.ui.widgets

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.supercoderguy.lucentlauncher.model.WeatherData
import io.github.supercoderguy.lucentlauncher.model.WeatherSettings
import io.github.supercoderguy.lucentlauncher.model.WeatherUnit
import io.github.supercoderguy.lucentlauncher.model.WidgetType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun LucentWidgetRenderer(
    type: WidgetType,
    weatherSettings: WeatherSettings = WeatherSettings(),
    weatherData: WeatherData? = null,
    onWeatherClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    useHaptic: Boolean = false
) {
    when (type) {
        WidgetType.CLOCK -> ClockWidget()
        WidgetType.SEARCH -> SearchWidget(onSearchClick, useHaptic)
        WidgetType.WEATHER -> {
            Log.d("LucentWidgetRender", "Rendering weather with data: $weatherData")
            WeatherWidget(weatherSettings, weatherData, onWeatherClick, useHaptic)
        }
        WidgetType.MUSIC -> MusicWidget(useHaptic)
    }
}

@Composable
fun ClockWidget() {
    var time by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now()
            delay(1000)
        }
    }

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    
    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = time.format(formatter),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SearchWidget(onClick: () -> Unit, useHaptic: Boolean) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .clickable { 
                if (useHaptic) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick() 
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("Search...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WeatherWidget(
    settings: WeatherSettings,
    data: WeatherData?,
    onClick: () -> Unit,
    useHaptic: Boolean
) {
    val haptic = LocalHapticFeedback.current
     Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { 
                if (useHaptic) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick() 
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tempText = if (data != null) {
            val temp = if (settings.unit == WeatherUnit.FAHRENHEIT) {
                (data.temperature * 9/5) + 32
            } else {
                data.temperature
            }
            "${temp.toInt()}°"
        } else {
            "--°"
        }

        Text(
            text = tempText,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = data?.condition ?: "Loading...",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MusicWidget(useHaptic: Boolean) {
    val haptic = LocalHapticFeedback.current
    
    // Simple placeholder for Alpha 0.3
    Surface(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Not Playing",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "No media session",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row {
                IconButton(onClick = { if (useHaptic) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null)
                }
                IconButton(onClick = { if (useHaptic) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
                IconButton(onClick = { if (useHaptic) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                }
            }
        }
    }
}
