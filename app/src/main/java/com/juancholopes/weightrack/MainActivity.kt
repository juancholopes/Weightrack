package com.juancholopes.weightrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.juancholopes.weightrack.ui.WeightrackApp
import com.juancholopes.weightrack.ui.theme.WeightrackTheme

/** The only activity (S30-R11): edge to edge, and the shell inside the theme (SF00-R05). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeightrackTheme { WeightrackApp() }
        }
    }
}
