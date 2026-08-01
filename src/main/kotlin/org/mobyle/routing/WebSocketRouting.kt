package org.mobyle.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import org.mobyle.data.remote.auth.authenticateJWT
import org.mobyle.data.service.WebSocketManager
import org.mobyle.data.service.WsTokenManager
import org.mobyle.di.injection
import org.mobyle.domain.usecase.auth.ValidateToken
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("WebSocketRouting")

fun Route.getWebSocketRouting() {
    val webSocketManager by injection<WebSocketManager>()
    val wsTokenManager by injection<WsTokenManager>()
    val validateToken by injection<ValidateToken>()

    // Step 1: Exchange JWT for a short-lived WS nonce token
    post("/ws/token") {
        val principal = call.authenticateJWT(validateToken) ?: return@post
        val userId = principal.claims.userId
        val nonce = wsTokenManager.generateToken(userId)

        log.info("[WS] Nonce token generated for user $userId")
        call.respond(HttpStatusCode.OK, mapOf("token" to nonce))
    }

    // Step 2: Connect WebSocket with nonce token
    webSocket("/ws") {
        val nonce = call.request.headers["authorization"]
            ?: call.parameters["token"]

        if (nonce.isNullOrBlank()) {
            log.warn("[WS] Connection rejected: missing token")
            close(CloseReason(4401, "Missing token"))
            return@webSocket
        }

        val userId = wsTokenManager.validateAndConsume(nonce)
        if (userId == null) {
            log.warn("[WS] Connection rejected: invalid or expired nonce")
            close(CloseReason(4401, "Invalid or expired token"))
            return@webSocket
        }

        log.info("[WS] User $userId authenticated and connected")
        webSocketManager.addConnection(userId, this)

        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    log.debug("[WS] Received from $userId: $text")
                }
            }
        } catch (e: Exception) {
            log.info("[WS] Connection closed for $userId: ${e.message}")
        } finally {
            webSocketManager.removeConnection(userId, this)
        }
    }
}
