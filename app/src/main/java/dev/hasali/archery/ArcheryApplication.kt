package dev.hasali.archery

import android.app.Application
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.hasali.archery.db.AppDatabase
import dev.hasali.archery.db.Sessions
import dev.hasali.archery.repository.SessionRepository
import dev.hasali.archery.repository.SettingsRepository
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

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context = this, driver = driver, databaseName = DATABASE_NAME)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }
}
