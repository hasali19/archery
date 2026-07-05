package dev.hasali.archery.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.hasali.archery.data.DistanceUnit
import dev.hasali.archery.data.DistanceValue
import dev.hasali.archery.data.RoundDetails
import dev.hasali.archery.data.RoundDistance
import dev.hasali.archery.data.ScoringSystems
import dev.hasali.archery.data.Session
import dev.hasali.archery.data.standardRoundsById
import dev.hasali.archery.db.AppDatabase
import dev.hasali.archery.db.Arrow_scores
import dev.hasali.archery.db.Sessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

sealed class NewSessionParams {
    data class Round(
        val roundId: String,
        val arrowsPerEnd: Int,
        val isCompetition: Boolean,
    ) : NewSessionParams()

    data class FreePractice(
        val arrowsPerEnd: Int,
        val distance: Int,
        val distanceUnit: DistanceUnit,
        val scoringSystem: String,
    ) : NewSessionParams()
}

class SessionRepository(
    private val database: AppDatabase,
) {
    fun watchSessions(): Flow<List<Session>> =
        combine(
            database.sessionQueries
                .selectAll()
                .asFlow()
                .mapToList(Dispatchers.IO),
            database.arrowScoreQueries
                .selectAll()
                .asFlow()
                .mapToList(Dispatchers.IO),
        ) { sessions, arrows ->
            val arrowsBySession = arrows.groupBy { it.sessionId }
            sessions.map { entity ->
                mapToSession(entity, arrowsBySession[entity.id] ?: emptyList())
            }
        }

    suspend fun getSession(sessionId: Int): Session =
        withContext(Dispatchers.IO) {
            val entity = database.sessionQueries.selectById(sessionId.toLong()).executeAsOne()
            val arrows =
                database.arrowScoreQueries.selectAllBySessionId(sessionId.toLong()).executeAsList()
            mapToSession(entity, arrows)
        }

    fun watchSession(sessionId: Int): Flow<Session?> =
        combine(
            database.sessionQueries
                .selectById(sessionId.toLong())
                .asFlow()
                .map { it.executeAsOneOrNull() },
            database.arrowScoreQueries
                .selectAllBySessionId(sessionId.toLong())
                .asFlow()
                .mapToList(Dispatchers.IO),
        ) { session, arrows ->
            session?.let { mapToSession(it, arrows) }
        }

    suspend fun createSession(params: NewSessionParams): Int =
        withContext(Dispatchers.IO) {
            database.transactionWithResult {
                val now = Clock.System.now()
                when (params) {
                    is NewSessionParams.Round -> {
                        database.sessionQueries.insert(
                            startTime = now,
                            roundId = params.roundId,
                            arrowsPerEnd = params.arrowsPerEnd.toString(),
                            distance = null,
                            distanceUnit = null,
                            scoringSystem = null,
                            isCompetition = params.isCompetition,
                        )
                    }

                    is NewSessionParams.FreePractice -> database.sessionQueries.insert(
                        startTime = now,
                        roundId = null,
                        arrowsPerEnd = params.arrowsPerEnd.toString(),
                        distance = params.distance.toLong(),
                        distanceUnit = params.distanceUnit.name,
                        scoringSystem = params.scoringSystem,
                        isCompetition = false,
                    )
                }
                database.sessionQueries
                    .lastInsertRowId()
                    .executeAsOne()
                    .toInt()
            }
        }

    suspend fun deleteSession(id: Int) =
        withContext(Dispatchers.IO) {
            database.sessionQueries.deleteById(id.toLong())
        }

    suspend fun insertScore(
        sessionId: Int,
        index: Int,
        scoreId: Int,
    ) = withContext(Dispatchers.IO) {
        database.arrowScoreQueries.insert(
            sessionId = sessionId.toLong(),
            index = index.toLong(),
            scoreId = scoreId.toLong(),
            timestamp = System.currentTimeMillis(),
        )
    }

    suspend fun deleteScore(
        sessionId: Int,
        index: Int,
    ) = withContext(Dispatchers.IO) {
        database.arrowScoreQueries.deleteByIndex(
            sessionId = sessionId.toLong(),
            index = index.toLong(),
        )
    }

    suspend fun updateStartTime(
        sessionId: Int,
        epochMillis: Instant,
    ) = withContext(Dispatchers.IO) {
        database.sessionQueries.updateStartTime(
            startTime = epochMillis,
            id = sessionId.toLong(),
        )
    }

    private fun mapToSession(
        entity: Sessions,
        arrows: List<Arrow_scores>,
    ): Session {
        val arrowsPerEnds = entity.arrowsPerEnd.split(",").map { it.trim().toIntOrNull() }

        val roundDetails: RoundDetails = if (entity.roundId != null) {
            val round = standardRoundsById[entity.roundId]!!
            var arrowsOffset = 0
            var endsOffset = 0
            val distances = round.distances
                .zip(arrowsPerEnds + List(round.distances.size) { null })
                .map { (distance, maybeArrowsPerEnd) ->
                    val arrowsPerEnd = maybeArrowsPerEnd ?: distance.defaultArrowsPerEnd
                    val ends = distance.arrows / arrowsPerEnd
                    val roundDistance = RoundDistance(
                        distanceValue = distance.distanceValue,
                        arrowsPerEnd = arrowsPerEnd,
                        ends = ends,
                        firstArrowIndex = arrowsOffset,
                        firstEndIndex = endsOffset,
                    )
                    arrowsOffset += distance.arrows
                    endsOffset += ends
                    roundDistance
                }

            RoundDetails(
                id = entity.roundId,
                displayName = round.displayName,
                scoringSystem = round.scoringSystem,
                distances = distances,
            )
        } else {
            val distanceValue = DistanceValue(
                value = entity.distance!!.toInt(),
                unit = DistanceUnit.valueOf(entity.distanceUnit!!),
            )
            val arrowsPerEnd = arrowsPerEnds.firstOrNull() ?: 3

            RoundDetails(
                id = null,
                displayName = "Free Practice • ${distanceValue.value} ${distanceValue.unit.name}",
                scoringSystem = ScoringSystems.byId[entity.scoringSystem] ?: ScoringSystems.metric,
                distances = listOf(
                    RoundDistance(
                        distanceValue = distanceValue,
                        arrowsPerEnd = arrowsPerEnd,
                        ends = 99,
                        firstArrowIndex = 0,
                        firstEndIndex = 0,
                    ),
                ),
            )
        }

        val scoringSystem = roundDetails.scoringSystem
        return Session(
            id = entity.id.toInt(),
            startTime = entity.startTime,
            roundDetails = roundDetails,
            scores = arrows.map { scoringSystem.get(it.scoreId.toInt()) },
            isCompetition = entity.isCompetition,
        )
    }
}
