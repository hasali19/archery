package dev.hasali.archery

import android.app.Application
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.hasali.archery.db.AppDatabase
import dev.hasali.archery.db.Sessions
import dev.hasali.archery.repository.SessionRepository
import kotlin.time.Instant

class ArcheryApplication : Application() {
    val database: AppDatabase by lazy {
        val driver = AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = this,
            name = "archery",
            callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA foreign_keys = ON")
                }
            },
        )

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
}
