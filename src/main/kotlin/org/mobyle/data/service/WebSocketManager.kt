package org.mobyle.data.service

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class WsMessage(
    val type: String,
    val payload: Map<String, String> = emptyMap()
)

class WebSocketManager {

    private val json = Json { encodeDefaults = true }

    // userId -> set of active sessions
    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()

    fun addConnection(userId: String, session: DefaultWebSocketSession) {
        connections.getOrPut(userId) { ConcurrentHashMap.newKeySet() }.add(session)
        println("[WS] User $userId connected (${connections[userId]?.size} sessions)")
    }

    fun removeConnection(userId: String, session: DefaultWebSocketSession) {
        connections[userId]?.remove(session)
        if (connections[userId]?.isEmpty() == true) {
            connections.remove(userId)
        }
        println("[WS] User $userId disconnected (${connections[userId]?.size ?: 0} sessions)")
    }

    suspend fun send(userId: String, message: WsMessage) {
        val sessions = connections[userId]
        if (sessions.isNullOrEmpty()) {
            println("[WS] No active sessions for user $userId, dropping message type=${message.type}")
            return
        }

        val text = json.encodeToString(message)
        println("[WS] Sending message type=${message.type} to user $userId (${sessions.size} sessions)")
        val deadSessions = mutableListOf<DefaultWebSocketSession>()

        for (session in sessions) {
            try {
                session.send(Frame.Text(text))
            } catch (e: Exception) {
                println("[WS] Failed to send to user $userId: ${e.message}")
                deadSessions.add(session)
            }
        }

        if (deadSessions.isNotEmpty()) {
            println("[WS] Cleaning up ${deadSessions.size} dead sessions for user $userId")
            deadSessions.forEach { removeConnection(userId, it) }
        }
    }
}
