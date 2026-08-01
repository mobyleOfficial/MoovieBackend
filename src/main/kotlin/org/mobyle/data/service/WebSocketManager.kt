package org.mobyle.data.service

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class WsMessage(
    val type: String,
    val payload: Map<String, String> = emptyMap()
)

class WebSocketManager {

    private val log = LoggerFactory.getLogger(WebSocketManager::class.java)
    private val json = Json { encodeDefaults = true }

    // userId -> set of active sessions
    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()

    fun addConnection(userId: String, session: DefaultWebSocketSession) {
        connections.getOrPut(userId) { ConcurrentHashMap.newKeySet() }.add(session)
        log.info("[WS] User $userId connected (${connections[userId]?.size} sessions)")
    }

    fun removeConnection(userId: String, session: DefaultWebSocketSession) {
        connections[userId]?.remove(session)
        if (connections[userId]?.isEmpty() == true) {
            connections.remove(userId)
        }
        log.info("[WS] User $userId disconnected (${connections[userId]?.size ?: 0} sessions)")
    }

    suspend fun send(userId: String, message: WsMessage) {
        val sessions = connections[userId] ?: return
        val text = json.encodeToString(message)
        val deadSessions = mutableListOf<DefaultWebSocketSession>()

        for (session in sessions) {
            try {
                session.send(Frame.Text(text))
            } catch (e: Exception) {
                log.warn("[WS] Failed to send to user $userId: ${e.message}")
                deadSessions.add(session)
            }
        }

        deadSessions.forEach { removeConnection(userId, it) }
    }
}
