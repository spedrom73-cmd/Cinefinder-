package com.cinefinder.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cinefinder.app.ui.CineFinderApp
import com.cinefinder.app.ui.Incoming
import com.cinefinder.app.ui.theme.CineFinderTheme

@Suppress("DEPRECATION")
private fun streamUri(i: Intent): Uri? =
    if (Build.VERSION.SDK_INT >= 33) {
        i.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        i.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val i = intent
        if (i != null && i.action == Intent.ACTION_SEND) {
            val uri = streamUri(i)
            if (uri != null) Incoming.uri = uri
        }
        setContent {
            CineFinderTheme {
                CineFinderApp()
            }
        }
    }
}
