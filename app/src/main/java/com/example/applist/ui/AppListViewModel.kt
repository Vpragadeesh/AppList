package com.example.applist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applist.data.AppRepository
import com.example.applist.data.FavoritesStore
import com.example.applist.data.SettingsStore
import com.example.applist.model.AppCategory
import com.example.applist.model.AppInfo
import com.example.applist.model.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppListUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val category: AppCategory = AppCategory.PIN,
    val sortOption: SortOption = SortOption.NAME,
    val pinned: Set<String> = emptySet(),
    val newAppsDetected: List<String> = emptyList(),
    val error: String? = null,
    val userCount: Int = 0,
    val systemCount: Int = 0,
    val showSystemApps: Boolean = false
)

class AppListViewModel(
    private val repo: AppRepository,
    private val favoritesStore: FavoritesStore,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppListUiState(
            pinned = favoritesStore.getAll(),
            showSystemApps = settingsStore.shouldShowSystemApps()
        )
    )
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    val repository: AppRepository get() = repo

    private var previousPackages: Set<String> = emptySet()

    private val _launchIntentCache = mutableMapOf<String, android.content.Intent?>()

    init {
        loadApps()
    }

    /**
     * Re-scans the installed app list. Called on launch and whenever the user
     * requests it (refresh button / pull-to-refresh / "Check for new apps").
     * Newly installed packages are reported in [AppListUiState.newAppsDetected].
     */
    fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val apps = repo.getInstalledApps()
                val packageSet = apps.map { it.packageName }.toSet()
                val newApps = if (previousPackages.isNotEmpty()) {
                    packageSet - previousPackages
                } else {
                    emptySet()
                }
                if (previousPackages.isEmpty()) {
                    previousPackages = packageSet
                }
                _launchIntentCache.clear()
                val userCount = apps.count { !it.isSystemApp }
                val systemCount = apps.count { it.isSystemApp }
                _uiState.update {
                    it.copy(
                        apps = apps,
                        isLoading = false,
                        newAppsDetected = newApps.toList(),
                        userCount = userCount,
                        systemCount = systemCount
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load apps")
                }
            }
        }
    }

    /**
     * Lazily loads the launch intent for [packageName].
     * Results are cached so repeated lookups are instant.
     */
    fun getLaunchIntent(packageName: String): android.content.Intent? {
        _launchIntentCache[packageName]?.let { return it }
        val intent = repo.getLaunchIntent(packageName)
        _launchIntentCache[packageName] = intent
        return intent
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setCategory(category: AppCategory) = _uiState.update { it.copy(category = category) }

    fun setSortOption(option: SortOption) = _uiState.update { it.copy(sortOption = option) }

    fun setShowSystemApps(show: Boolean) {
        settingsStore.setShowSystemApps(show)
        _uiState.update { it.copy(showSystemApps = show) }
    }

    fun togglePinned(packageName: String) {
        val pinned = favoritesStore.toggle(packageName)
        _uiState.update { it.copy(pinned = pinned) }
    }
}
