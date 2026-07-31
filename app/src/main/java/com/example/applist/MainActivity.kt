package com.example.applist

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.applist.data.AppRepository
import com.example.applist.data.FavoritesStore
import com.example.applist.ui.AppDetailsScreen
import com.example.applist.ui.AppListScreen
import com.example.applist.ui.AppListViewModel
import com.example.applist.ui.theme.AppListTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppListViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                AppListViewModel(
                    repo = AppRepository(app),
                    favoritesStore = FavoritesStore(app)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppListTheme {
                AppListApp(viewModel)
            }
        }
    }
}

@Composable
private fun AppListApp(viewModel: AppListViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            AppListScreen(
                viewModel = viewModel,
                onAppClick = { app ->
                    navController.navigate("details/${app.packageName}")
                }
            )
        }
        composable("details/{packageName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName")
            AppDetailsScreen(
                viewModel = viewModel,
                packageName = packageName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
