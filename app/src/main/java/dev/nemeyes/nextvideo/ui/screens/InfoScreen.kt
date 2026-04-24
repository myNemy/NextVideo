package dev.nemeyes.nextvideo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nemeyes.nextvideo.R

@Composable
fun InfoScreen(
    modifier: Modifier = Modifier,
    /** OCS Theming [name] for the selected account’s server, if available. */
    instanceName: String? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.info_app_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.info_app_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!instanceName.isNullOrBlank()) {
            Text(
                stringResource(R.string.info_instance_name, instanceName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

