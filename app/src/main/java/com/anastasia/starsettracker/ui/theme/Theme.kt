package com.anastasia.starsettracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun StarSetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (false) darkColorScheme() else lightColorScheme(),
        content = content
    )
}
