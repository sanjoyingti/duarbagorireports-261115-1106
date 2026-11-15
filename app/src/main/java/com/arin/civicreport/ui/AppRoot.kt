package com.arin.civicreport.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.ui.screens.AuthoritiesScreen
import com.arin.civicreport.ui.screens.CaptureScreen
import com.arin.civicreport.ui.screens.ComposeScreen
import com.arin.civicreport.ui.screens.HomeScreen
import com.arin.civicreport.ui.screens.ReportDetailScreen
import com.arin.civicreport.ui.screens.ReportsScreen
import com.arin.civicreport.ui.screens.SettingsScreen
import com.arin.civicreport.ui.screens.TypePickerScreen
import com.arin.civicreport.viewmodel.ReportViewModel

@Composable
fun AppRoot() {
    val vm: ReportViewModel = viewModel()
    val navController = rememberNavController()
    var splashDone by rememberSaveable { mutableStateOf(false) }
    val settings by vm.settings.collectAsStateWithLifecycle()

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(settings.notificationsEnabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            settings.notificationsEnabled &&
            !vm.notifications.canNotify()
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!splashDone) {
        SplashScreen({ splashDone = true })
        return
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onNewReport = { type ->
                    vm.clearDraft()
                    if (type != null) vm.toggleType(type)
                    navController.navigate(Routes.TYPES)
                },
                onReports = { navController.navigate(Routes.REPORTS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenReport = { id -> navController.navigate(Routes.reportDetail(id)) }
            )
        }

        composable(Routes.TYPES) {
            TypePickerScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.CAPTURE) }
            )
        }

        composable(Routes.CAPTURE) {
            CaptureScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.AUTHORITIES) }
            )
        }

        composable(Routes.AUTHORITIES) {
            AuthoritiesScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.COMPOSE) }
            )
        }

        composable(Routes.COMPOSE) {
            ComposeScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onFinished = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.REPORTS) {
            ReportsScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onOpenReport = { id -> navController.navigate(Routes.reportDetail(id)) }
            )
        }

        composable(
            route = Routes.REPORT_DETAIL,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) { entry ->
            ReportDetailScreen(
                vm = vm,
                reportId = entry.arguments?.getString("reportId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}

/** Suggestion chips on the home screen use this to preselect a type. */
fun preselect(vm: ReportViewModel, type: IncidentType) {
    vm.toggleType(type)
}
