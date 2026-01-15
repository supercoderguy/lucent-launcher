package io.github.supercoderguy.lucentlauncher.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.supercoderguy.lucentlauncher.data.AppRepository
import io.github.supercoderguy.lucentlauncher.data.PersistenceRepository
import io.github.supercoderguy.lucentlauncher.data.WeatherRepository
import io.github.supercoderguy.lucentlauncher.model.DockConfig
import io.github.supercoderguy.lucentlauncher.model.DockEdge
import io.github.supercoderguy.lucentlauncher.model.DockItem
import io.github.supercoderguy.lucentlauncher.model.LauncherSettings
import io.github.supercoderguy.lucentlauncher.model.LauncherState
import io.github.supercoderguy.lucentlauncher.model.ThemeMode
import io.github.supercoderguy.lucentlauncher.model.WeatherSettings
import io.github.supercoderguy.lucentlauncher.model.WeatherUnit
import io.github.supercoderguy.lucentlauncher.model.WidgetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val appRepository = AppRepository(application)
    private val persistenceRepository = PersistenceRepository(application)
    private val weatherRepository = WeatherRepository()
    
    private val _uiState = MutableStateFlow(LauncherState())
    val uiState: StateFlow<LauncherState> = _uiState.asStateFlow()

    init {
        loadApps()
        loadPersistentState()
    }

    private fun loadApps() {
        viewModelScope.launch {
            appRepository.getInstalledApps().collect { apps ->
                _uiState.update { it.copy(installedApps = apps) }
            }
        }
    }

    private fun loadPersistentState() {
        viewModelScope.launch {
            val savedState = persistenceRepository.loadState()
            if (savedState != null) {
                _uiState.update { state ->
                    state.copy(
                        docks = savedState.docks,
                        themeMode = savedState.themeMode,
                        useDynamicColor = savedState.useDynamicColor,
                        weatherSettings = savedState.weatherSettings,
                        settings = savedState.settings
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        docks = listOf(
                            DockConfig(id = "dock_bottom", edge = DockEdge.BOTTOM),
                            DockConfig(id = "home_screen", edge = DockEdge.FLOATING)
                        )
                    )
                }
            }
            refreshWeather()
        }
    }

    private fun saveState() {
        viewModelScope.launch {
            persistenceRepository.saveState(_uiState.value)
        }
    }

    fun refreshWeather() {
        val location = _uiState.value.weatherSettings.location
        viewModelScope.launch {
            val data = weatherRepository.fetchWeather(location)
            if (data != null) {
                _uiState.update { it.copy(weatherData = data) }
            }
        }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode, isAppPickerVisible = false, isSettingsVisible = false) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        saveState()
    }

    fun updateLauncherSettings(settings: LauncherSettings) {
        _uiState.update { it.copy(settings = settings) }
        saveState()
    }

    fun toggleAppDrawer() {
        _uiState.update { it.copy(isAppDrawerVisible = !it.isAppDrawerVisible) }
    }

    fun openAppPicker(dockId: String) {
        _uiState.update { it.copy(isAppPickerVisible = true, targetDockId = dockId) }
    }

    fun closeAppPicker() {
        _uiState.update { it.copy(isAppPickerVisible = false, targetDockId = null) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsVisible = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsVisible = false) }
    }

    fun openWeatherSettings() {
        _uiState.update { it.copy(isWeatherSettingsVisible = true) }
    }

    fun closeWeatherSettings() {
        _uiState.update { it.copy(isWeatherSettingsVisible = false) }
    }

    fun updateWeatherSettings(location: String, unit: WeatherUnit) {
        _uiState.update { it.copy(
            weatherSettings = WeatherSettings(location, unit),
            isWeatherSettingsVisible = false
        ) }
        saveState()
        refreshWeather()
    }

    fun launchApp(app: DockItem.App) {
        _uiState.update { it.copy(isAppDrawerVisible = false) }
        appRepository.launchApp(app)
    }

    fun addAppToDock(app: DockItem.App) {
        val dockId = _uiState.value.targetDockId ?: return
        _uiState.update { state ->
            val updatedDocks = state.docks.map { dock ->
                if (dock.id == dockId) {
                    dock.copy(items = dock.items + app)
                } else {
                    dock
                }
            }
            state.copy(docks = updatedDocks, isAppPickerVisible = false, targetDockId = null)
        }
        saveState()
    }

    fun addWidgetToDock(type: WidgetType) {
        val dockId = _uiState.value.targetDockId ?: return
        val widget = DockItem.InternalWidget(type)
        _uiState.update { state ->
            val updatedDocks = state.docks.map { dock ->
                if (dock.id == dockId) {
                    dock.copy(items = dock.items + widget)
                } else {
                    dock
                }
            }
            state.copy(docks = updatedDocks, isAppPickerVisible = false, targetDockId = null)
        }
        saveState()
        if (type == WidgetType.WEATHER) refreshWeather()
    }

    fun removeAppFromDock(dockId: String, app: DockItem) {
        _uiState.update { state ->
            val updatedDocks = state.docks.map { dock ->
                if (dock.id == dockId) {
                    dock.copy(items = dock.items - app)
                } else {
                    dock
                }
            }
            state.copy(docks = updatedDocks)
        }
        saveState()
    }
}
