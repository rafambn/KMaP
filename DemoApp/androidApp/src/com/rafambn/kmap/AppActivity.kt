package com.rafambn.kmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vectorDemo = intent.getBooleanExtra("vector", false)
            App(initialRoute = if (vectorDemo) Routes.VectorTiles else Routes.Start, vectorZoom = if (vectorDemo) 14F else 0F)
        }
    }
}
