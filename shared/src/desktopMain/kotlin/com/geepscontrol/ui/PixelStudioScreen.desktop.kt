package com.geepscontrol.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PixelStudioScreen(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Text("Pixel Studio image import and export are currently available in the Android app.")
    }
}
