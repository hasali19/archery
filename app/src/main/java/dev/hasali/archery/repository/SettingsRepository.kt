package dev.hasali.archery.repository

import android.content.Context
import android.net.Uri
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

// The suffixes sqlite uses for its main database file's WAL-mode sidecar files.
private val DB_FILE_SUFFIXES = listOf("", "-wal", "-shm", "-journal")

// The first 16 bytes of every valid sqlite database file: the ASCII string "SQLite format 3"
// followed by a null terminator byte.
// See: https://www.sqlite.org/fileformat.html#the_database_header
private val SQLITE_HEADER = "SQLite format 3".toByteArray(Charsets.US_ASCII) + byteArrayOf(0)

class SettingsRepository(
    private val context: Context,
    private val driver: SqlDriver,
    private val databaseName: String,
) {
    suspend fun exportDatabase(destination: Uri) =
        withContext(Dispatchers.IO) {
            // Ensure all writes are flushed from the WAL into the main database file so
            // that the exported copy is self-contained and up to date.
            val checkpointResult = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA wal_checkpoint(TRUNCATE);",
                mapper = { QueryResult.Value(Unit) },
                parameters = 0,
            )
            checkpointResult.await()

            val dbFile = context.getDatabasePath(databaseName)

            context.contentResolver.openOutputStream(destination)?.use { output ->
                dbFile.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Unable to open output stream for $destination")
        }

    /**
     * Replaces the app's database with the file at [source]. The driver is closed as part of
     * this, so the caller must restart the app (or otherwise recreate the database/driver)
     * afterwards - the existing database connection can no longer be used once this returns,
     * whether it succeeds or fails.
     */
    suspend fun importDatabase(source: Uri) =
        withContext(Dispatchers.IO) {
            requireValidSqliteFile(source)

            // Close the existing connection so nothing is still holding the database files
            // open while we overwrite them below.
            driver.close()

            val dbFile = context.getDatabasePath(databaseName)
            for (suffix in DB_FILE_SUFFIXES) {
                File(dbFile.path + suffix).delete()
            }

            context.contentResolver.openInputStream(source)?.use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IOException("Unable to open input stream for $source")
        }

    private fun requireValidSqliteFile(source: Uri) {
        val header = ByteArray(SQLITE_HEADER.size)
        val stream = context.contentResolver.openInputStream(source)
            ?: throw IOException("Unable to open input stream for $source")

        val bytesRead = stream.use { input ->
            var total = 0
            while (total < header.size) {
                val n = input.read(header, total, header.size - total)
                if (n < 0) break
                total += n
            }
            total
        }

        if (bytesRead < header.size || !header.contentEquals(SQLITE_HEADER)) {
            throw IOException("Selected file is not a valid SQLite database")
        }
    }
}
