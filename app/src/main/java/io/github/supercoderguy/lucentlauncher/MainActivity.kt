package io.github.supercoderguy.lucentlauncher

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.supercoderguy.lucentlauncher.model.DockConfig
import io.github.supercoderguy.lucentlauncher.model.DockEdge
import io.github.supercoderguy.lucentlauncher.model.DockItem
import io.github.supercoderguy.lucentlauncher.model.LauncherSettings
import io.github.supercoderguy.lucentlauncher.model.ThemeMode
import io.github.supercoderguy.lucentlauncher.model.WeatherUnit
import io.github.supercoderguy.lucentlauncher.model.WidgetType
import io.github.supercoderguy.lucentlauncher.ui.LauncherViewModel
import io.github.supercoderguy.lucentlauncher.ui.theme.LucentLauncherTheme
import io.github.supercoderguy.lucentlauncher.ui.widgets.LucentWidgetRenderer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LauncherViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            val context = LocalContext.current

            LucentLauncherTheme(
                isPureMinimal = state.themeMode == ThemeMode.PURE_MINIMAL,
                dynamicColor = state.useDynamicColor
            ) {
                LucentCanvas(
                    state = state,
                    onLongPress = { viewModel.toggleEditMode() },
                    onAppClick = { viewModel.launchApp(it) },
                    onAddClick = { viewModel.openAppPicker(it) },
                    onRemoveApp = { dockId, item -> viewModel.removeAppFromDock(dockId, item) },
                    onAppSelected = { viewModel.addAppToDock(it) },
                    onWidgetSelected = { viewModel.addWidgetToDock(it) },
                    onDismissPicker = { viewModel.closeAppPicker() },
                    onThemeToggle = {
                        val nextMode = if (state.themeMode == ThemeMode.DEFAULT) ThemeMode.PURE_MINIMAL else ThemeMode.DEFAULT
                        viewModel.setThemeMode(nextMode)
                    },
                    onSwipeUp = { viewModel.toggleAppDrawer() },
                    onCloseDrawer = { viewModel.toggleAppDrawer() },
                    onWeatherClick = { viewModel.openWeatherSettings() },
                    onUpdateWeather = { loc, unit -> viewModel.updateWeatherSettings(loc, unit) },
                    onDismissWeather = { viewModel.closeWeatherSettings() },
                    onSearchClick = {
                        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(SearchManager.QUERY, "")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val browserIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_APP_BROWSER)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(browserIntent)
                        }
                    },
                    onOpenSettings = { viewModel.openSettings() },
                    onUpdateSettings = { viewModel.updateLauncherSettings(it) },
                    onDismissSettings = { viewModel.closeSettings() }
                )
            }
        }
    }
}

@Composable
fun LucentCanvas(
    state: io.github.supercoderguy.lucentlauncher.model.LauncherState,
    onLongPress: () -> Unit,
    onAppClick: (DockItem.App) -> Unit,
    onAddClick: (String) -> Unit,
    onRemoveApp: (String, DockItem) -> Unit,
    onAppSelected: (DockItem.App) -> Unit,
    onWidgetSelected: (WidgetType) -> Unit,
    onDismissPicker: () -> Unit,
    onThemeToggle: () -> Unit,
    onSwipeUp: () -> Unit,
    onCloseDrawer: () -> Unit,
    onWeatherClick: () -> Unit,
    onUpdateWeather: (String, WeatherUnit) -> Unit,
    onDismissWeather: () -> Unit,
    onSearchClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onDismissSettings: () -> Unit
) {
    val scale by animateFloatAsState(if (state.isEditMode) 0.92f else 1f, label = "canvasScale")
    val isPureMinimal = state.themeMode == ThemeMode.PURE_MINIMAL

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isPureMinimal) Color.Black else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { if (state.isAppDrawerVisible) onCloseDrawer() }
                )
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    if (delta < -20 && !state.isAppDrawerVisible && !state.isEditMode && !isPureMinimal) {
                        onSwipeUp()
                    }
                }
            )
    ) {
        if (isPureMinimal) {
            PureMinimalView(
                apps = state.installedApps,
                onAppClick = onAppClick
            )
        } else {
            // Main Launcher Surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
            ) {
                // Render Docks
                state.docks.forEach { dockConfig ->
                    DockView(
                        config = dockConfig,
                        isEditMode = state.isEditMode,
                        onAppClick = onAppClick,
                        onAddClick = { onAddClick(dockConfig.id) },
                        onRemoveItem = { onRemoveApp(dockConfig.id, it) },
                        state = state,
                        onWeatherClick = onWeatherClick,
                        onSearchClick = onSearchClick
                    )
                }
            }
        }

        // App Drawer Overlay
        AnimatedVisibility(
            visible = state.isAppDrawerVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AppDrawer(
                apps = state.installedApps,
                onAppClick = onAppClick,
                onClose = onCloseDrawer
            )
        }

        // Weather Settings Dialog
        if (state.isWeatherSettingsVisible) {
            WeatherSettingsDialog(
                currentLocation = state.weatherSettings.location,
                currentUnit = state.weatherSettings.unit,
                onSave = onUpdateWeather,
                onDismiss = onDismissWeather
            )
        }

        // Launcher Settings Dialog
        if (state.isSettingsVisible) {
            LauncherSettingsDialog(
                settings = state.settings,
                onSave = onUpdateSettings,
                onDismiss = onDismissSettings
            )
        }

        // Edit Mode indicator / Exit button
        if (state.isEditMode && !state.isAppPickerVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onLongPress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Done Editing")
                }
                
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        // Item Picker Overlay (Apps + Widgets)
        AnimatedVisibility(
            visible = state.isAppPickerVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ItemPicker(
                apps = state.installedApps,
                onAppSelected = onAppSelected,
                onWidgetSelected = onWidgetSelected,
                onDismiss = onDismissPicker
            )
        }
    }
}

@Composable
fun LauncherSettingsDialog(
    settings: LauncherSettings,
    onSave: (LauncherSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var hapticEnabled by remember { mutableStateOf(settings.useHapticFeedback) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Lucent Settings", style = MaterialTheme.typography.headlineSmall)
                
                ListItem(
                    headlineContent = { Text("Haptic Feedback") },
                    supportingContent = { Text("Physical vibration on interactions") },
                    trailingContent = {
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { hapticEnabled = it }
                        )
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onSave(LauncherSettings(useHapticFeedback = hapticEnabled)) }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AppDrawer(
    apps: List<DockItem.App>,
    onAppClick: (DockItem.App) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredApps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAppClick(app) }
                            .padding(4.dp)
                    ) {
                        AppIconImage(
                            app = app,
                            size = 56.dp
                        )
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PureMinimalView(
    apps: List<DockItem.App>,
    onAppClick: (DockItem.App) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 64.dp)
    ) {
        Text(
            "Apps",
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(apps) { app ->
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppClick(app) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DockView(
    config: DockConfig,
    isEditMode: Boolean,
    onAppClick: (DockItem.App) -> Unit,
    onAddClick: () -> Unit,
    onRemoveItem: (DockItem) -> Unit,
    state: io.github.supercoderguy.lucentlauncher.model.LauncherState,
    onWeatherClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val alignment = when (config.edge) {
        DockEdge.TOP -> Alignment.TopCenter
        DockEdge.BOTTOM -> Alignment.BottomCenter
        DockEdge.LEFT -> Alignment.CenterStart
        DockEdge.RIGHT -> Alignment.CenterEnd
        DockEdge.FLOATING -> Alignment.Center
    }

    val isHorizontal = config.edge in listOf(DockEdge.TOP, DockEdge.BOTTOM, DockEdge.FLOATING)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (config.edge == DockEdge.BOTTOM) 16.dp else 24.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 6.dp,
            modifier = Modifier.wrapContentSize()
        ) {
            if (isHorizontal) {
                if (config.edge == DockEdge.FLOATING) {
                    Column(
                        modifier = Modifier.padding(8.dp).widthIn(max = 320.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val chunkedItems = config.items.chunked(4)
                        chunkedItems.forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                rowItems.forEach { item ->
                                    DockItemRenderer(item, isEditMode, onAppClick, onRemoveItem, state, onWeatherClick, onSearchClick)
                                }
                            }
                        }
                        if (isEditMode) {
                            Spacer(Modifier.height(8.dp))
                            AddButton(onAddClick)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        config.items.forEach { item ->
                            DockItemRenderer(item, isEditMode, onAppClick, onRemoveItem, state, onWeatherClick, onSearchClick)
                        }
                        if (isEditMode) AddButton(onAddClick)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    config.items.forEach { item ->
                        DockItemRenderer(item, isEditMode, onAppClick, onRemoveItem, state, onWeatherClick, onSearchClick)
                    }
                    if (isEditMode) AddButton(onAddClick)
                }
            }
        }
    }
}

@Composable
fun DockItemRenderer(
    item: DockItem,
    isEditMode: Boolean,
    onAppClick: (DockItem.App) -> Unit,
    onRemoveItem: (DockItem) -> Unit,
    state: io.github.supercoderguy.lucentlauncher.model.LauncherState,
    onWeatherClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Box(contentAlignment = Alignment.TopEnd) {
        when (item) {
            is DockItem.App -> {
                AppIcon(
                    app = item,
                    isEditMode = isEditMode,
                    onClick = { if (!isEditMode) onAppClick(item) }
                )
            }
            is DockItem.InternalWidget -> {
                LucentWidgetRenderer(
                    type = item.type,
                    weatherSettings = state.weatherSettings,
                    weatherData = state.weatherData,
                    onWeatherClick = onWeatherClick,
                    onSearchClick = onSearchClick,
                    useHaptic = state.settings.useHapticFeedback
                )
            }
        }

        if (isEditMode) {
            Surface(
                onClick = { onRemoveItem(item) },
                modifier = Modifier
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(20.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                tonalElevation = 4.dp
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
}

@Composable
fun AddButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(50.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Item")
    }
}

@Composable
fun AppIconImage(app: DockItem.App, size: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val pm = context.packageManager
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    
    val icon = remember(app.packageName, app.componentName) {
        try {
            val component = ComponentName(app.packageName, app.componentName)
            val activityInfo = launcherApps.getActivityList(app.packageName, Process.myUserHandle())
                .find { it.componentName == component }
            
            if (activityInfo != null) {
                activityInfo.getIcon(0).toBitmap().asImageBitmap()
            } else {
                pm.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(1),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun AppIcon(
    app: DockItem.App,
    isEditMode: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        AppIconImage(app = app, size = 52.dp)
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ItemPicker(
    apps: List<DockItem.App>,
    onAppSelected: (DockItem.App) -> Unit,
    onWidgetSelected: (WidgetType) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .pointerInput(Unit) { /* Consume taps */ },
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Add to Launcher",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Widgets Section
                Text("Lucent Widgets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WidgetPickerItem("Clock", WidgetType.CLOCK) { onWidgetSelected(it) }
                    WidgetPickerItem("Search", WidgetType.SEARCH) { onWidgetSelected(it) }
                    WidgetPickerItem("Weather", WidgetType.WEATHER) { onWidgetSelected(it) }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Apps Section
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredApps) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
                            leadingContent = {
                                AppIconImage(app = app, size = 40.dp)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAppSelected(app) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPickerItem(label: String, type: WidgetType, onClick: (WidgetType) -> Unit) {
    Surface(
        onClick = { onClick(type) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.width(80.dp).height(60.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun WeatherSettingsDialog(
    currentLocation: String,
    currentUnit: WeatherUnit,
    onSave: (String, WeatherUnit) -> Unit,
    onDismiss: () -> Unit
) {
    var location by remember { mutableStateOf(currentLocation) }
    var unit by remember { mutableStateOf(currentUnit) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Weather Settings", style = MaterialTheme.typography.headlineSmall)
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Temperature Unit", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = unit == WeatherUnit.CELSIUS,
                        onClick = { unit = WeatherUnit.CELSIUS },
                        label = { Text("Celsius") }
                    )
                    FilterChip(
                        selected = unit == WeatherUnit.FAHRENHEIT,
                        onClick = { unit = WeatherUnit.FAHRENHEIT },
                        label = { Text("Fahrenheit") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onSave(location, unit) }) { Text("Save") }
                }
            }
        }
    }
}
