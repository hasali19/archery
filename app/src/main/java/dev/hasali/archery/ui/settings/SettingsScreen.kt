package dev.hasali.archery.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private val exportFileNameFormat = kotlinx.datetime.LocalDateTime.Format {
    year()
    char('-')
    monthNumber()
    char('-')
    day()
    char('-')
    hour()
    minute()
    second()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.sqlite3"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportDatabase(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                SettingsEvent.ExportSucceeded -> "Database exported"
                SettingsEvent.ExportFailed -> "Failed to export database"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ListItem(
                headlineContent = { Text("Export database") },
                supportingContent = { Text("Save a copy of the app database to a file") },
                leadingContent = {
                    Icon(Icons.Filled.Storage, contentDescription = null)
                },
                modifier = Modifier
                    .clickable {
                        val now = Clock.System.now()
                        val timestamp = now
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .format(exportFileNameFormat)
                        exportLauncher.launch("archery-$timestamp.db")
                    },
            )
        }
    }
}
