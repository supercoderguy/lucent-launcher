package io.github.supercoderguy.lucentlauncher.data

import android.content.Context
import io.github.supercoderguy.lucentlauncher.model.LauncherState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PersistenceRepository(private val context: Context) {
    private val fileName = "launcher_state.json"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun saveState(state: LauncherState) = withContext(Dispatchers.IO) {
        try {
            // We only want to persist docks and theme mode, not UI flags like isEditMode
            val persistentState = state.copy(
                isEditMode = false,
                isAppPickerVisible = false,
                isAppDrawerVisible = false,
                targetDockId = null,
                installedApps = emptyList()
            )
            val jsonString = json.encodeToString(persistentState)
            File(context.filesDir, fileName).writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadState(): LauncherState? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val jsonString = file.readText()
                json.decodeFromString<LauncherState>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
