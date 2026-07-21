package com.geepscontrol.ui

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit
