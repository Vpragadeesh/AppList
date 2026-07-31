package com.example.applist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applist.data.AppRepository
import com.example.applist.data.FavoritesStore
import com.example.applist.model.AppCategory
import com.example.applist.model.AppInfo
import com.example.applist.model.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppListUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val category: AppCategory = AppCategory.PIN,
    val sortOption: SortOption = SortOption.NAME,
    val pinned: Set<String> = emptySet(),
    val newAppsDetected: List<String> = emptyList(),
    val error: String? = null,
    val launchIntents: Map<String, android.content.Intent> = emptyMap(),
    val userCount: Int = 0,
    val systemCount: Int = 0
)

class AppListViewModel(
    private val repo: AppRepository,
    private val favoritesStore: FavoritesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState(pinned = favoritesStore.getAll()))
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    val repository: AppRepository get() = repo

    private var previousPackages: Set<String> = emptySet()

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
                previousPackages = packageSet
                // Pre-compute launch intents on background thread
                val launchIntents = withContext(Dispatchers.IO) {
                    apps.mapNotNull { app ->
                        repo.getLaunchIntent(app.packageName)?.let { app.packageName to it }
                    }.toMap()
                }
                val userCount = apps.count { !it.isSystemApp }
                val systemCount = apps.count { it.isSystemApp }
                _uiState.update {
                    it.copy(
                        apps = apps,
                        isLoading = false,
                        newAppsDetected = newApps.toList(),
                        launchIntents = launchIntents,
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

    private var searchJob: kotlinx.coroutines.Job? = null

    fun setSearchQuery(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300) // debounce
            _uiState.update { it.copy(searchQuery = query) }
        }
    }

    fun setCategory(category: AppCategory) = _uiState.update { it.copy(category = category) }

    fun setSortOption(option: SortOption) = _uiState.update { it.copy(sortOption = option) }

    fun togglePinned(packageName: String) {
        val pinned = favoritesStore.toggle(packageName)
        _uiState.update { it.copy(pinned = pinned) }
    }
}
