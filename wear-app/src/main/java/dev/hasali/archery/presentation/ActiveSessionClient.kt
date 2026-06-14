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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.nio.ByteBuffer

data class ArrowScore(
    val id: Int,
    val label: String,
    val color: Color,
) {
    val foregroundColor: Color
        get() = if (color.luminance() > 0.5f) Color.Black else Color.White
}

data class ArcherySession(
    val sessionId: Int,
    val totalScore: Int,
    val name: String,
    val currentDistance: Pair<Int, String>?,
    val endScores: List<ArrowScore>,
    val keyboardScores: List<ArrowScore>,
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
    fun observeSession(): Flow<ArcherySessionState> =
        callbackFlow {
            val dataClient = Wearable.getDataClient(context)
            val listener = DataClient.OnDataChangedListener { events ->
                for (event in events) {
                    if (event.dataItem.uri.path == "/active-session") {
                        trySend(
                            if (event.type == DataEvent.TYPE_DELETED) {
                                ArcherySessionState.Inactive
                            } else {
                                ArcherySessionState.Active(parseSession(event.dataItem))
                            },
                        )
                    }
                }
            }
            dataClient.addListener(listener)
            dataClient
                .getDataItems("wear://*/active-session".toUri())
                .addOnSuccessListener { items ->
                    trySend(
                        if (items.count > 0) {
                            ArcherySessionState.Active(parseSession(items[0]))
                        } else {
                            ArcherySessionState.Inactive
                        },
                    )
                    items.release()
                }
            awaitClose { dataClient.removeListener(listener) }
        }

    fun addScore(
        sessionId: Int,
        scoreId: Int,
    ) {
        sendMessage(
            "/score/add",
            ByteBuffer
                .allocate(8)
                .putInt(sessionId)
                .putInt(scoreId)
                .array(),
        )
    }

    fun deleteLastScore(sessionId: Int) {
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
        val endScores = endLabels
            .zip(endColors)
            .map { (label, color) -> ArrowScore(0, label, Color(color)) }

        val keyIds = dataMap.getIntegerArrayList("keyboardScoreIds") ?: arrayListOf()
        val keyLabels = dataMap.getStringArray("keyboardScoreLabels") ?: emptyArray()
        val keyColors = dataMap.getIntegerArrayList("keyboardScoreColors") ?: arrayListOf()
        val keyboardScores = keyIds.indices
            .map { i -> ArrowScore(keyIds[i], keyLabels[i], Color(keyColors[i])) }

        return ArcherySession(
            sessionId = dataMap.getInt("sessionId"),
            totalScore = dataMap.getInt("totalScore"),
            name = dataMap.getString("sessionName") ?: "",
            currentDistance = if (dataMap.containsKey("currentDistanceValue"))
                Pair(dataMap.getInt("currentDistanceValue"), dataMap.getString("currentDistanceUnit") ?: "Metres")
            else null,
            endScores = endScores,
            keyboardScores = keyboardScores,
        )
    }
}
