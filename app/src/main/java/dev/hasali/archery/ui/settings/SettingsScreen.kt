package dev.hasali.archery.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.hasali.archery.util.restartApp
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImportUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.sqlite3"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportDatabase(uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Import database") },
            text = {
                Text(
                    "This will replace all existing sessions with the data from the " +
                        "selected file. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingImportUri!!
                        pendingImportUri = null
                        viewModel.importDatabase(uri)
                    },
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.ExportSucceeded -> snackbarHostState.showSnackbar("Database exported")
                SettingsEvent.ExportFailed -> snackbarHostState.showSnackbar("Failed to export database")
                // The database connection is no longer usable at this point, so restart the app
                // to pick up the newly imported data from scratch.
                SettingsEvent.ImportSucceeded -> restartApp(context)
                SettingsEvent.ImportFailed -> snackbarHostState.showSnackbar("Failed to import database")
            }
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
                    Icon(Icons.Filled.Download, contentDescription = null)
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
            ListItem(
                headlineContent = { Text("Import database") },
                supportingContent = { Text("Replace the app database from a file") },
                leadingContent = {
                    Icon(Icons.Filled.Upload, contentDescription = null)
                },
                modifier = Modifier
                    .clickable { importLauncher.launch(arrayOf("*/*")) },
            )
        }
    }
}
