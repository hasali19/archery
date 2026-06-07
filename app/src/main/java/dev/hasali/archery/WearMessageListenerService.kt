package dev.hasali.archery

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dev.hasali.archery.repository.SessionRepository
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer

class WearMessageListenerService : WearableListenerService() {
    private lateinit var sessionRepository: SessionRepository

    override fun onCreate() {
        super.onCreate()
        sessionRepository = (application as ArcheryApplication).sessionRepository
    }

    override fun onMessageReceived(messageEvent: MessageEvent) =
        runBlocking {
            val buffer = ByteBuffer.wrap(messageEvent.data)
            val sessionId = buffer.int

            when (messageEvent.path) {
                "/score/add" -> {
                    val scoreId = buffer.int
                    val session = sessionRepository.getSession(sessionId)
                    sessionRepository.insertScore(sessionId, session.scores.size, scoreId)
                }
                "/score/delete-last" -> {
                    val session = sessionRepository.getSession(sessionId)
                    if (session.scores.isNotEmpty()) {
                        sessionRepository.deleteScore(sessionId, session.scores.size - 1)
                    }
                }
            }
        }
}
