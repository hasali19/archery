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

data class WearScore(
    val id: Int,
    val label: String,
    val color: Color,
) {
    val foregroundColor: Color
        get() = if (color.luminance() > 0.5f) Color.Black else Color.White
}

data class WearSessionState(
    val sessionId: Int,
    val endScores: List<WearScore>,
    val keyboardScores: List<WearScore>,
)

sealed interface WearSessionObservation {
    data object Loading : WearSessionObservation

    data object Inactive : WearSessionObservation

    data class Active(
        val state: WearSessionState,
    ) : WearSessionObservation
}

class PhoneSessionClient(
    private val context: Context,
) {
    fun observeSession(): Flow<WearSessionObservation> =
        callbackFlow {
            val dataClient = Wearable.getDataClient(context)
            val listener = DataClient.OnDataChangedListener { events ->
                for (event in events) {
                    if (event.dataItem.uri.path == "/active-session") {
                        trySend(
                            if (event.type == DataEvent.TYPE_DELETED) {
                                WearSessionObservation.Inactive
                            } else {
                                WearSessionObservation.Active(parseSessionState(event.dataItem))
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
                            WearSessionObservation.Active(parseSessionState(items[0]))
                        } else {
                            WearSessionObservation.Inactive
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

    private fun parseSessionState(dataItem: DataItem): WearSessionState {
        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap

        val endLabels = dataMap.getStringArray("endScoreLabels") ?: emptyArray()
        val endColors = dataMap.getIntegerArrayList("endScoreColors") ?: arrayListOf()
        val endScores = endLabels
            .zip(endColors)
            .map { (label, color) -> WearScore(0, label, Color(color)) }

        val keyIds = dataMap.getIntegerArrayList("keyboardScoreIds") ?: arrayListOf()
        val keyLabels = dataMap.getStringArray("keyboardScoreLabels") ?: emptyArray()
        val keyColors = dataMap.getIntegerArrayList("keyboardScoreColors") ?: arrayListOf()
        val keyboardScores = keyIds.indices
            .map { i -> WearScore(keyIds[i], keyLabels[i], Color(keyColors[i])) }

        return WearSessionState(
            sessionId = dataMap.getInt("sessionId"),
            endScores = endScores,
            keyboardScores = keyboardScores,
        )
    }
}
