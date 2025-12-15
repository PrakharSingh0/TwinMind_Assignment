package com.example.twinmind.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.twinmind.ui.screens.meetinglist.MeetingListScreen
import com.example.twinmind.ui.screens.permissions.PermissionScreen
import com.example.twinmind.ui.screens.recording.RecordingScreen
import com.example.twinmind.ui.screens.summary.SummaryScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.PERMISSIONS
    ) {
        composable(NavRoutes.PERMISSIONS) {
            PermissionScreen(onPermissionsGranted = {
                navController.navigate(NavRoutes.MEETING_LIST) {
                    popUpTo(NavRoutes.PERMISSIONS) { inclusive = true }
                }
            })
        }

        // Home / meeting list
        composable(NavRoutes.MEETING_LIST) {
            MeetingListScreen(
                onStartRecording = { navController.navigate(NavRoutes.RECORDING) },
                onOpenMeeting = { meetingId ->
                    // 🔹 navigate to summary/<id>
                    navController.navigate("${NavRoutes.SUMMARY}/$meetingId")
                }
            )
        }

        // Recording screen
        composable(NavRoutes.RECORDING) {
            RecordingScreen(
                onBack = { navController.popBackStack() },
                onFinished = { meetingId ->
                    if (meetingId > 0) {
                        navController.navigate("${NavRoutes.SUMMARY}/$meetingId") {
                            // 🔥 FIX: Pop the recording screen off the back stack
                            popUpTo(NavRoutes.RECORDING) {
                                inclusive = true
                            }
                        }
                    }
                }
            )
        }

        // Summary screen with argument
        composable(
            route = "${NavRoutes.SUMMARY}/{meetingId}",     // 🔹 route pattern
            arguments = listOf(
                navArgument("meetingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val meetingId =
                backStackEntry.arguments?.getLong("meetingId") ?: 0L

            SummaryScreen(
                meetingId = meetingId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
