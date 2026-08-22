package dev.hasali.archery.repository

import android.content.Context
import android.net.Uri
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class SettingsRepository(
    private val context: Context,
    private val driver: SqlDriver,
    private val databaseName: String,
) {
    suspend fun exportDatabase(destination: Uri) =
        withContext(Dispatchers.IO) {
            // Ensure all writes are flushed from the WAL into the main database file so
            // that the exported copy is self-contained and up to date. This has to be run
            // as a query rather than an update/exec, since PRAGMA wal_checkpoint returns a
            // result row (busy, log, checkpointed) and Android's SQLiteStatement rejects
            // statements that return rows when executed via executeUpdateDelete.
            val checkpointResult = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA wal_checkpoint(TRUNCATE);",
                mapper = { QueryResult.Value(Unit) },
                parameters = 0,
            )
            checkpointResult.value

            val dbFile = context.getDatabasePath(databaseName)

            context.contentResolver.openOutputStream(destination)?.use { output ->
                dbFile.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Unable to open output stream for $destination")
        }
}
