package dev.nemeyes.nextvideo.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * App bar colors aligned with the official [Nextcloud Android](https://github.com/nextcloud/android) light
 * look: `appbar` = white, `text_color` / `fontAppbar`-style title and icons — not a solid primary bar.
 * Uses [androidx.compose.material3.ColorScheme] in dark theme so text stays readable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ncAppBarTopColors() =
    TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
