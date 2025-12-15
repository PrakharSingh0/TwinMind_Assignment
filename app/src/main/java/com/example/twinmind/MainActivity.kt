package com.example.twinmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.twinmind.ui.navigation.AppNavHost
import com.example.twinmind.ui.theme.TwinMindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        Graph.provide(this)  // <-- add this line

        setContent {
            AppRoot()
        }
    }
}


@Composable
fun AppRoot() {
    TwinMindTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            AppNavHost(navController = navController)
        }
    }
}
