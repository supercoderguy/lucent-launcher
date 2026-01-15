package io.github.supercoderguy.lucentlauncher.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

@Serializable
sealed interface DockItem {
    @Serializable
    data class App(
        val label: String,
        val packageName: String,
        val componentName: String
    ) : DockItem

    @Serializable
    data class InternalWidget(
        val type: WidgetType
    ) : DockItem
}

@Serializable
enum class WidgetType {
    CLOCK,
    WEATHER,
    SEARCH,
    MUSIC
}

@Serializable
enum class WeatherUnit {
    CELSIUS, FAHRENHEIT
}

@Serializable
data class WeatherSettings(
    val location: String = "London",
    val unit: WeatherUnit = WeatherUnit.FAHRENHEIT
)

@Serializable
data class WeatherData(
    val temperature: Float,
    val condition: String
)

@Serializable
enum class DockEdge {
    TOP, BOTTOM, LEFT, RIGHT, FLOATING
}

@Serializable
data class DockConfig(
    val id: String,
    val edge: DockEdge,
    val items: List<DockItem> = emptyList(),
    // size and positionOffset excluded from simple serialization for now
    // size: Dp = 80.dp,
    val positionOffset: Float = 0.5f 
)

@Serializable
enum class ThemeMode {
    DEFAULT, PURE_MINIMAL
}

@Serializable
data class LauncherSettings(
    val useHapticFeedback: Boolean = false
)

@Serializable
data class LauncherState(
    val docks: List<DockConfig> = emptyList(),
    val isEditMode: Boolean = false,
    val isAppPickerVisible: Boolean = false,
    val isAppDrawerVisible: Boolean = false,
    val isWeatherSettingsVisible: Boolean = false,
    val isSettingsVisible: Boolean = false,
    val targetDockId: String? = null,
    val installedApps: List<DockItem.App> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val useDynamicColor: Boolean = true,
    val weatherSettings: WeatherSettings = WeatherSettings(),
    val weatherData: WeatherData? = null,
    val settings: LauncherSettings = LauncherSettings()
)
