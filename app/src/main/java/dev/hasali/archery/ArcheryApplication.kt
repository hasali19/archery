package dev.hasali.archery

import android.app.Application
import android.net.Uri
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.hasali.archery.db.AppDatabase
import dev.hasali.archery.db.Sessions
import dev.hasali.archery.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant

private const val DATABASE_NAME = "archery"

class ArcheryApplication : Application() {
    private val driver: AndroidSqliteDriver by lazy {
        AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = this,
            name = DATABASE_NAME,
            callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            },
        )
    }

    val database: AppDatabase by lazy {
        AppDatabase(
            driver = driver,
            sessionsAdapter = Sessions.Adapter(
                startTimeAdapter = object : ColumnAdapter<Instant, Long> {
                    override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)

                    override fun encode(value: Instant) = value.toEpochMilliseconds()
                },
            ),
        )
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(database)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }

    /**
     * Copies the underlying sqlite database file to [destination], which is typically a
     * document Uri obtained from a Storage Access Framework "create document" picker.
     */
    suspend fun exportDatabaseTo(destination: Uri) =
        withContext(Dispatchers.IO) {
            // Ensure any pending writes in the write-ahead log are flushed into the main
            // database file before it's copied, so the export isn't missing recent changes.
            driver.execute(null, "PRAGMA wal_checkpoint(FULL);", 0).value

            val dbFile = getDatabasePath(DATABASE_NAME)
            contentResolver.openOutputStream(destination)?.use { output ->
                dbFile.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Failed to open output stream for $destination")
        }
}
