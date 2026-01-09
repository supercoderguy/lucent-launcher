package io.github.supercoderguy.lucentlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.supercoderguy.lucentlauncher.model.DockConfig
import io.github.supercoderguy.lucentlauncher.model.DockEdge
import io.github.supercoderguy.lucentlauncher.model.DockItem
import io.github.supercoderguy.lucentlauncher.model.ThemeMode
import io.github.supercoderguy.lucentlauncher.ui.LauncherViewModel
import io.github.supercoderguy.lucentlauncher.ui.theme.LucentLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LauncherViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            LucentLauncherTheme(
                isPureMinimal = state.themeMode == ThemeMode.PURE_MINIMAL
            ) {
                LucentCanvas(
                    state = state,
                    onLongPress = { viewModel.toggleEditMode() },
                    onAppClick = { viewModel.launchApp(it) },
                    onAddClick = { viewModel.openAppPicker(it) },
                    onRemoveApp = { dockId, app -> viewModel.removeAppFromDock(dockId, app) },
                    onAppSelected = { viewModel.addAppToDock(it) },
                    onDismissPicker = { viewModel.closeAppPicker() },
                    onThemeToggle = {
                        val nextMode = if (state.themeMode == ThemeMode.DEFAULT) ThemeMode.PURE_MINIMAL else ThemeMode.DEFAULT
                        viewModel.setThemeMode(nextMode)
                    },
                    onSwipeUp = { viewModel.toggleAppDrawer() },
                    onCloseDrawer = { viewModel.toggleAppDrawer() }
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
    onRemoveApp: (String, DockItem.App) -> Unit,
    onAppSelected: (DockItem.App) -> Unit,
    onDismissPicker: () -> Unit,
    onThemeToggle: () -> Unit,
    onSwipeUp: () -> Unit,
    onCloseDrawer: () -> Unit
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
                        onRemoveApp = { onRemoveApp(dockConfig.id, it) }
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

        // Edit Mode indicator / Exit button
        if (state.isEditMode && !state.isAppPickerVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                
                OutlinedButton(
                    onClick = onThemeToggle,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(if (isPureMinimal) "Switch to Default" else "Switch to Pure Minimal")
                }
            }
        }

        // App Picker Overlay
        AnimatedVisibility(
            visible = state.isAppPickerVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AppPicker(
                apps = state.installedApps,
                onAppSelected = onAppSelected,
                onDismiss = onDismissPicker
            )
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
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.label.take(1),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
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
    onRemoveApp: (DockItem.App) -> Unit
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
            .padding(24.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            tonalElevation = 6.dp,
            modifier = Modifier.wrapContentSize()
        ) {
            if (isHorizontal) {
                // If floating (Home Screen), we use a flow-like arrangement for multiple apps
                if (config.edge == DockEdge.FLOATING) {
                    Column(
                        modifier = Modifier.padding(16.dp).widthIn(max = 300.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Using a simple wrapped row logic
                        val chunkedItems = config.items.chunked(4)
                        chunkedItems.forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    DockItemRenderer(item, isEditMode, onAppClick, onRemoveApp)
                                }
                            }
                        }
                        if (isEditMode) {
                            AddButton(onAddClick)
                        } else if (config.items.isEmpty()) {
                            Text("Long-press to edit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        config.items.forEach { item ->
                            DockItemRenderer(item, isEditMode, onAppClick, onRemoveApp)
                        }
                        if (isEditMode) AddButton(onAddClick)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    config.items.forEach { item ->
                        DockItemRenderer(item, isEditMode, onAppClick, onRemoveApp)
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
    onRemoveApp: (DockItem.App) -> Unit
) {
    when (item) {
        is DockItem.App -> {
            AppIcon(
                app = item,
                isEditMode = isEditMode,
                onClick = { if (!isEditMode) onAppClick(item) },
                onRemove = { onRemoveApp(item) }
            )
        }
        is DockItem.InternalWidget -> { /* Handle widgets later */ }
    }
}

@Composable
fun AddButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add Item")
    }
}

@Composable
fun AppIcon(
    app: DockItem.App,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Box(contentAlignment = Alignment.TopEnd) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isEditMode) {
            Surface(
                onClick = onRemove,
                modifier = Modifier
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                tonalElevation = 4.dp
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun AppPicker(
    apps: List<DockItem.App>,
    onAppSelected: (DockItem.App) -> Unit,
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
                .fillMaxHeight(0.8f)
                .pointerInput(Unit) { /* Consume taps */ },
            shape = RoundedCornerShape(32.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Add Application",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
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
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(app.label.take(1))
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAppSelected(app) }
                        )
                    }
                    
                    if (filteredApps.isEmpty()) {
                        item {
                            Text(
                                "No apps found",
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
