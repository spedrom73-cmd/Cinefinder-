package com.cinefinder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cinefinder.app.ui.CineFinderScreen
import com.cinefinder.app.ui.theme.CineFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CineFinderTheme {
                CineFinderScreen()
            }
        }
    }
}