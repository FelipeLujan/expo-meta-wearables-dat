package expo.modules.emwdat

import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.core.session.DeviceSessionState
import com.meta.wearable.dat.display.Display
import com.meta.wearable.dat.display.addDisplay
import com.meta.wearable.dat.display.removeDisplay
import com.meta.wearable.dat.display.types.DisplayConfiguration
import com.meta.wearable.dat.display.types.DisplayError
import com.meta.wearable.dat.display.types.DisplayState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

object DisplaySessionManager {
    private val logger = EMWDATLogger

    private val displays: MutableMap<String, Display> = mutableMapOf()
    private val stateJobs: MutableMap<String, Job> = mutableMapOf()
    private val attachMutexes: MutableMap<String, Mutex> = mutableMapOf()

    private var scope: CoroutineScope? = null
    private var eventEmitter: EventEmitter? = null

    fun setEventEmitter(emitter: EventEmitter) {
        eventEmitter = emitter
    }

    fun setScope(s: CoroutineScope) {
        scope = s
    }

    /** Attach display to session. Idempotent — safe to call when already attached. */
    suspend fun addDisplayToSession(sessionId: String) {
        ensureDisplayAttached(sessionId)
    }

    fun removeDisplayFromSession(sessionId: String) {
        val display = displays[sessionId]
        display?.stop()
        val session = WearablesManager.getSession(sessionId)
        session?.removeDisplay()
        destroyDisplay(sessionId)
        emitEvent("onDisplayStateChange", mapOf("sessionId" to sessionId, "state" to "stopped"))
        logger.info("Display", "Display removed from session", mapOf("sessionId" to sessionId))
    }

    /**
     * Sends content to the display. Auto-attaches and waits for session/display readiness
     * when no display is attached yet (DisplayViewModel.send pattern).
     */
    suspend fun sendDisplayContent(sessionId: String, contentTree: Map<String, Any>) {
        ensureDisplayAttached(sessionId)

        val display = displays[sessionId]
            ?: throw IllegalArgumentException("No active display for session: $sessionId")

        if (display.state.value != DisplayState.STARTED) {
            waitForDisplayStarted(display)
        }

        val result = display.sendContent {
            DisplayContentBuilder.build(this, contentTree) { interactionId ->
                emitEvent(
                    "onDisplayInteraction",
                    mapOf("sessionId" to sessionId, "interactionId" to interactionId)
                )
            }
        }

        result.fold(
            onSuccess = { /* sent */ },
            onFailure = { error, _ ->
                val errorCode = mapDisplayError(error)
                emitEvent("onDisplayError", mapOf("sessionId" to sessionId, "error" to errorCode))
                throw Exception("sendDisplayContent failed: $error")
            }
        )
    }

    private suspend fun ensureDisplayAttached(sessionId: String) {
        if (displays.containsKey(sessionId)) return

        val mutex = attachMutexes.getOrPut(sessionId) { Mutex() }
        mutex.withLock {
            if (displays.containsKey(sessionId)) return

            val session = WearablesManager.getSession(sessionId)
                ?: throw IllegalArgumentException("Session not found: $sessionId")

            waitForSessionStarted(session)

            var display: Display? = null
            session.addDisplay(DisplayConfiguration()).fold(
                onSuccess = { d -> display = d },
                onFailure = { error, _ -> throw Exception("Failed to add display: $error") }
            )
            val activeDisplay = display ?: throw Exception("Failed to add display to session")
            displays[sessionId] = activeDisplay

            val currentScope = scope ?: throw IllegalStateException("Module scope not available")
            stateJobs[sessionId] = currentScope.launch {
                activeDisplay.state.collect { state ->
                    handleStateChange(sessionId, state)
                }
            }

            logger.info("Display", "Display attached to session", mapOf("sessionId" to sessionId))
        }
    }

    private suspend fun waitForSessionStarted(session: DeviceSession, timeoutMs: Long = 45_000) {
        if (session.state.value == DeviceSessionState.STARTED) return

        withTimeout(timeoutMs) {
            session.state.first { state ->
                when (state) {
                    DeviceSessionState.STARTED -> true
                    DeviceSessionState.STOPPED ->
                        throw IllegalStateException("Session stopped before starting")
                    else -> false
                }
            }
        }
    }

    private suspend fun waitForDisplayStarted(display: Display, timeoutMs: Long = 45_000) {
        if (display.state.value == DisplayState.STARTED) return

        withTimeout(timeoutMs) {
            var sawStarting = false
            display.state.first { state ->
                when (state) {
                    DisplayState.STARTING, DisplayState.STOPPING -> {
                        sawStarting = true
                        false
                    }
                    DisplayState.STARTED -> true
                    DisplayState.STOPPED -> {
                        if (sawStarting) {
                            throw IllegalStateException("Display stopped before starting")
                        }
                        false
                    }
                    else -> false
                }
            }
        }
    }

    private fun handleStateChange(sessionId: String, state: DisplayState) {
        val mapped = mapDisplayState(state)
        logger.info("Display", "State changed", mapOf("sessionId" to sessionId, "state" to mapped))
        emitEvent("onDisplayStateChange", mapOf("sessionId" to sessionId, "state" to mapped))

        // Only tear down local refs when capability is fully removed — not on transient STOPPED
        // (allows re-send via sendDisplayContent auto-attach after removeDisplayFromSession).
        if (state == DisplayState.CLOSED) {
            destroyDisplay(sessionId)
        }
    }

    private fun destroyDisplay(sessionId: String) {
        stateJobs[sessionId]?.cancel()
        stateJobs.remove(sessionId)
        displays.remove(sessionId)
        attachMutexes.remove(sessionId)
    }

    fun destroy() {
        for (sessionId in displays.keys.toList()) {
            destroyDisplay(sessionId)
        }
    }

    private fun mapDisplayState(state: DisplayState): String = when (state) {
        DisplayState.STOPPED -> "stopped"
        DisplayState.STARTING -> "starting"
        DisplayState.STARTED -> "started"
        DisplayState.STOPPING -> "stopping"
        DisplayState.CLOSED -> "stopped"
        else -> "stopped"
    }

    private fun mapDisplayError(error: DisplayError): String = when (error) {
        DisplayError.CAPABILITY_DENIED -> "capabilityDenied"
        DisplayError.DEVICE_DISCONNECTED -> "deviceDisconnected"
        DisplayError.INVALID_SESSION_STATE -> "invalidSessionState"
        DisplayError.RENDERING_FAILED -> "renderingFailed"
        else -> "unexpectedError"
    }

    private fun emitEvent(name: String, body: Map<String, Any>) {
        eventEmitter?.invoke(name, body)
    }
}
