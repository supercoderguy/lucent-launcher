package io.github.supercoderguy.lucentlauncher.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

sealed interface DockItem {
    data class App(
        val label: String,
        val packageName: String,
        val componentName: String
    ) : DockItem

    data class InternalWidget(
        val type: WidgetType
    ) : DockItem
}

enum class WidgetType {
    CLOCK,
    WEATHER,
    SEARCH
}

enum class DockEdge {
    TOP, BOTTOM, LEFT, RIGHT, FLOATING
}

data class DockConfig(
    val id: String,
    val edge: DockEdge,
    val items: List<DockItem> = emptyList(),
    val size: Dp = 80.dp,
    val positionOffset: Float = 0.5f // 0.0 to 1.0 along the edge
)

enum class ThemeMode {
    DEFAULT, PURE_MINIMAL
}

data class LauncherState(
    val docks: List<DockConfig> = emptyList(),
    val isEditMode: Boolean = false,
    val isAppPickerVisible: Boolean = false,
    val isAppDrawerVisible: Boolean = false,
    val targetDockId: String? = null,
    val installedApps: List<DockItem.App> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.DEFAULT,
    val useDynamicColor: Boolean = true
)
