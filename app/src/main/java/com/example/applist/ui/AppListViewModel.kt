package com.example.applist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applist.data.AppRepository
import com.example.applist.data.FavoritesStore
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
    val category: AppCategory = AppCategory.ALL,
    val sortOption: SortOption = SortOption.NAME,
    val showSystemApps: Boolean = true,
    val favorites: Set<String> = emptySet(),
    val newAppsDetected: List<String> = emptyList(),
    val error: String? = null
)

class AppListViewModel(
    private val repo: AppRepository,
    private val favoritesStore: FavoritesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState(favorites = favoritesStore.getAll()))
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
                _uiState.update {
                    it.copy(apps = apps, isLoading = false, newAppsDetected = newApps.toList())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load apps")
                }
            }
        }
    }

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun setCategory(category: AppCategory) = _uiState.update { it.copy(category = category) }

    fun setSortOption(option: SortOption) = _uiState.update { it.copy(sortOption = option) }

    fun setShowSystemApps(show: Boolean) = _uiState.update { it.copy(showSystemApps = show) }

    fun toggleFavorite(packageName: String) {
        val favorites = favoritesStore.toggle(packageName)
        _uiState.update { it.copy(favorites = favorites) }
    }
}
