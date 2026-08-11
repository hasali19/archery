package dev.hasali.archery

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dev.hasali.archery.data.DistanceValue
import dev.hasali.archery.data.Score
import dev.hasali.archery.data.Session

class WearSessionPublisher(
    private val context: Context,
) {
    fun publish(
        sessionId: Int,
        session: Session,
    ) {
        val endScores = getCurrentEndScores(session)
        val keyboard = session.roundDetails.scoringSystem.scores
        val request = PutDataMapRequest
            .create("/active-session")
            .apply {
                dataMap.putInt("sessionId", sessionId)
                dataMap.putInt("totalScore", session.scores.sumOf { it.value })
                dataMap.putString("sessionName", session.roundDetails.displayName)
                val dist = getCurrentDistance(session)
                if (dist != null) {
                    dataMap.putInt("currentDistanceValue", dist.value)
                    dataMap.putString("currentDistanceUnit", dist.unit.name)
                }
                dataMap.putStringArray("endScoreLabels", endScores.map { it.label }.toTypedArray())
                dataMap.putIntegerArrayList("endScoreColors", ArrayList(endScores.map { it.color.toArgb() }))
                dataMap.putIntegerArrayList("endScoreValues", ArrayList(endScores.map { it.value }))
                dataMap.putInt("currentArrowsPerEnd", getCurrentArrowsPerEnd(session))
                dataMap.putIntegerArrayList("keyboardScoreIds", ArrayList(keyboard.map { it.id }))
                dataMap.putStringArray("keyboardScoreLabels", keyboard.map { it.label }.toTypedArray())
                dataMap.putIntegerArrayList("keyboardScoreColors", ArrayList(keyboard.map { it.color.toArgb() }))
                dataMap.putIntegerArrayList("keyboardScoreValues", ArrayList(keyboard.map { it.value }))
            }.asPutDataRequest()
            .setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
    }

    fun clear() {
        Wearable.getDataClient(context).deleteDataItems("wear://*/active-session".toUri())
    }

    private fun getCurrentArrowsPerEnd(session: Session): Int {
        val totalScores = session.scores.size
        val distances = session.roundDetails.distances
        return distances.lastOrNull { it.firstArrowIndex <= totalScores }?.arrowsPerEnd ?: 0
    }

    private fun getCurrentDistance(session: Session): DistanceValue? {
        val totalScores = session.scores.size
        val distances = session.roundDetails.distances
        return distances.lastOrNull { it.firstArrowIndex <= totalScores }?.distanceValue
    }

    private fun getCurrentEndScores(session: Session): List<Score> {
        val totalScores = session.scores.size
        if (totalScores == 0) return emptyList()

        val distances = session.roundDetails.distances
        val currentDistance = distances.lastOrNull { it.firstArrowIndex < totalScores }
            ?: return emptyList()

        val arrowsPerEnd = currentDistance.arrowsPerEnd
        val scoresInDistance = totalScores - currentDistance.firstArrowIndex
        val endIndexInDistance = (scoresInDistance - 1) / arrowsPerEnd
        val currentEndStart = currentDistance.firstArrowIndex + endIndexInDistance * arrowsPerEnd

        return session.scores.drop(currentEndStart).take(arrowsPerEnd)
    }
}
