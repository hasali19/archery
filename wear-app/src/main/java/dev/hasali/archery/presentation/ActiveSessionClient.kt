package dev.hasali.archery.presentation

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.core.net.toUri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

enum class DistanceUnit { Metres, Yards }

data class CurrentDistance(val value: Int, val unit: DistanceUnit)

data class ArrowScore(
    val id: Int,
    val value: Int,
    val label: String,
    val color: Color,
) {
    val foregroundColor: Color
        get() = if (color.luminance() > 0.5f) Color.Black else Color.White
}

data class ArcherySession(
    val sessionId: Int,
    val totalScore: Int,
    val name: String?,
    val currentDistance: CurrentDistance?,
    val hasMultipleDistances: Boolean,
    val endScores: List<ArrowScore>,
    val keyboardScores: List<ArrowScore>,
    val arrowsPerEnd: Int,
)

sealed interface ArcherySessionState {
    data object Loading : ArcherySessionState

    data object Inactive : ArcherySessionState

    data class Active(
        val session: ArcherySession,
    ) : ArcherySessionState
}

class ActiveSessionClient(
    private val context: Context,
) {
    private val _sessionState = MutableStateFlow<ArcherySessionState>(ArcherySessionState.Loading)
    val sessionState: StateFlow<ArcherySessionState> = _sessionState.asStateFlow()

    private var dataListener: DataClient.OnDataChangedListener? = null

    fun startListening() {
        val dataClient = Wearable.getDataClient(context)
        val listener = DataClient.OnDataChangedListener { events ->
            for (event in events) {
                if (event.dataItem.uri.path == "/active-session") {
                    _sessionState.value = if (event.type == DataEvent.TYPE_DELETED) {
                        ArcherySessionState.Inactive
                    } else {
                        ArcherySessionState.Active(parseSession(event.dataItem))
                    }
                }
            }
        }
        dataListener = listener
        dataClient.addListener(listener)
        dataClient
            .getDataItems("wear://*/active-session".toUri())
            .addOnSuccessListener { items ->
                _sessionState.value = if (items.count > 0) {
                    ArcherySessionState.Active(parseSession(items[0]))
                } else {
                    ArcherySessionState.Inactive
                }
                items.release()
            }
    }

    fun stopListening() {
        dataListener?.let { Wearable.getDataClient(context).removeListener(it) }
        dataListener = null
    }

    fun addScore(sessionId: Int, score: ArrowScore) {
        val current = _sessionState.value
        if (current is ArcherySessionState.Active) {
            val session = current.session
            val newEndScores = if (session.arrowsPerEnd > 0 && session.endScores.size >= session.arrowsPerEnd) {
                listOf(score)
            } else {
                session.endScores + score
            }
            _sessionState.value = ArcherySessionState.Active(
                session.copy(
                    totalScore = session.totalScore + score.value,
                    endScores = newEndScores,
                ),
            )
        }
        sendMessage(
            "/score/add",
            ByteBuffer
                .allocate(8)
                .putInt(sessionId)
                .putInt(score.id)
                .array(),
        )
    }

    fun deleteLastScore(sessionId: Int) {
        val current = _sessionState.value
        if (current is ArcherySessionState.Active) {
            val session = current.session
            if (session.endScores.isNotEmpty()) {
                val lastScore = session.endScores.last()
                _sessionState.value = ArcherySessionState.Active(
                    session.copy(
                        totalScore = session.totalScore - lastScore.value,
                        endScores = session.endScores.dropLast(1),
                    ),
                )
            }
        }
        sendMessage("/score/delete-last", ByteBuffer.allocate(4).putInt(sessionId).array())
    }

    private fun sendMessage(
        path: String,
        payload: ByteArray,
    ) {
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            val node = nodes.firstOrNull() ?: return@addOnSuccessListener
            Wearable.getMessageClient(context).sendMessage(node.id, path, payload)
        }
    }

    private fun parseSession(dataItem: DataItem): ArcherySession {
        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap

        val endLabels = dataMap.getStringArray("endScoreLabels") ?: emptyArray()
        val endColors = dataMap.getIntegerArrayList("endScoreColors") ?: arrayListOf()
        val endValues = dataMap.getIntegerArrayList("endScoreValues") ?: arrayListOf()
        val endScores = endLabels.indices.map { i ->
            ArrowScore(
                id = 0,
                value = endValues.getOrElse(i) { 0 },
                label = endLabels[i],
                color = Color(endColors[i]),
            )
        }

        val keyIds = dataMap.getIntegerArrayList("keyboardScoreIds") ?: arrayListOf()
        val keyLabels = dataMap.getStringArray("keyboardScoreLabels") ?: emptyArray()
        val keyColors = dataMap.getIntegerArrayList("keyboardScoreColors") ?: arrayListOf()
        val keyValues = dataMap.getIntegerArrayList("keyboardScoreValues") ?: arrayListOf()
        val keyboardScores = keyIds.indices.map { i ->
            ArrowScore(
                id = keyIds[i],
                value = keyValues.getOrElse(i) { 0 },
                label = keyLabels[i],
                color = Color(keyColors[i]),
            )
        }

        return ArcherySession(
            sessionId = dataMap.getInt("sessionId"),
            totalScore = dataMap.getInt("totalScore"),
            name = dataMap.getString("sessionName"),
            currentDistance = if (dataMap.containsKey("currentDistanceValue"))
                CurrentDistance(
                    value = dataMap.getInt("currentDistanceValue"),
                    unit = DistanceUnit.valueOf(dataMap.getString("currentDistanceUnit") ?: "Metres"),
                )
            else null,
            hasMultipleDistances = dataMap.getBoolean("hasMultipleDistances"),
            endScores = endScores,
            keyboardScores = keyboardScores,
            arrowsPerEnd = dataMap.getInt("currentArrowsPerEnd"),
        )
    }
}
