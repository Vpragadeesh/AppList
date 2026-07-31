package com.example.applist.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.applist.model.AppCategory
import com.example.applist.model.AppInfo
import com.example.applist.model.SortOption
import com.example.applist.util.AppFilter
import com.example.applist.util.ExportUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onAppClick: (AppInfo) -> Unit,
    onNavigateToRam: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filteredApps = remember(state.apps, state.searchQuery, state.category, state.sortOption, state.pinned) {
        AppFilter.apply(
            state.apps,
            state.searchQuery,
            state.category,
            state.sortOption,
            state.pinned
        )
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = { viewModel.loadApps() }
    )

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.newAppsDetected) {
        if (state.newAppsDetected.isNotEmpty()) {
            val names = state.newAppsDetected.take(3).joinToString(", ")
            val suffix = if (state.newAppsDetected.size > 3) " and ${state.newAppsDetected.size - 3} more" else ""
            snackbarHostState.showSnackbar("New app${if (state.newAppsDetected.size > 1) "s" else ""} found: $names$suffix")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AppList") },
                actions = {
                    IconButton(onClick = { viewModel.loadApps() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    SortMenu(
                        current = state.sortOption,
                        onSelect = viewModel::setSortOption,
                        onCheckForNewApps = viewModel::loadApps
                    )
                    IconButton(onClick = { shareApps(context, state.apps) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share app list")
                    }
                    IconButton(onClick = onNavigateToRam) {
                        Icon(Icons.Filled.Settings, contentDescription = "RAM usage")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or package") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )
            CategoryTabs(state.category, onSelect = viewModel::setCategory)
            StatsBar(state)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                when {
                    state.isLoading && state.apps.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.error != null && state.apps.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            state.error?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                            Spacer(Modifier.height(12.dp))
                            Text("Tap refresh to retry",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    filteredApps.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (state.category == AppCategory.PIN) {
                                    "No pinned apps yet\nTap the star on any app to pin it"
                                } else {
                                    "No apps match your filters"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val launchIntent = state.launchIntents[app.packageName]
                        AppListItem(
                            app = app,
                            isPinned = app.packageName in state.pinned,
                            onLaunch = {
                                launchIntent?.let { context.startActivity(it) }
                            },
                            onDetailsClick = { onAppClick(app) },
                            onTogglePin = { viewModel.togglePinned(app.packageName) },
                            iconLoader = viewModel.repository::loadIcon
                        )
                    }
                }
            }
                }
                PullRefreshIndicator(
                    refreshing = state.isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortMenu(
    current: SortOption,
    onSelect: (SortOption) -> Unit,
    onCheckForNewApps: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (option == current) {
                            Icon(Icons.Filled.Check, contentDescription = "Selected")
                        }
                    }
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Check for new apps") },
                onClick = {
                    onCheckForNewApps()
                    expanded = false
                },
                leadingIcon = {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryTabs(selected: AppCategory, onSelect: (AppCategory) -> Unit) {
    val selectedIndex = AppCategory.entries.indexOf(selected)
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 16.dp
    ) {
        AppCategory.entries.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(category) },
                text = { Text(category.label) }
            )
        }
    }
}

@Composable
private fun StatsBar(state: AppListUiState) {
    Text(
        text = "${state.apps.size} apps  •  ${state.userCount} user  •  ${state.systemCount} system",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

private fun shareApps(context: Context, apps: List<AppInfo>) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Installed apps")
        putExtra(Intent.EXTRA_TEXT, ExportUtils.toCsv(apps))
    }
    context.startActivity(Intent.createChooser(intent, "Share app list"))
}
