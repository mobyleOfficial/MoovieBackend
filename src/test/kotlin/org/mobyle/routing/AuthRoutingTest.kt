package org.mobyle.routing

import org.mobyle.domain.model.OAuthCallbackRequest
import kotlin.test.Test
import kotlin.test.assertTrue

class AuthRoutingTest {

    @Test
    fun testOAuthCallbackRequestParsing() {
        val request = OAuthCallbackRequest(
            code = "test-code",
            state = "test-state",
            codeVerifier = null
        )

        assertTrue(request.code.isNotBlank())
        assertTrue(request.state.isNotBlank())
    }

    @Test
    fun testOAuthCallbackRequestWithCodeVerifier() {
        val request = OAuthCallbackRequest(
            code = "test-code",
            state = "test-state",
            codeVerifier = "test-verifier"
        )

        assertTrue(request.code.isNotBlank())
        assertTrue(request.state.isNotBlank())
        assertTrue(request.codeVerifier != null)
    }
}
