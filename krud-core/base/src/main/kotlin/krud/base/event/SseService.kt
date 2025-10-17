/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import krud.base.env.Tracer

/**
 * Service for managing and broadcasting events using Server-Sent Events (SSE).
 */
public object SseService {
    private val tracer: Tracer = Tracer<SseService>()

    /**
     * The number of events to replay when a new subscriber connects.
     * This determines how many past events are sent to new subscribers.
     */
    private const val REPLAY: Int = 100

    /**
     * The buffer capacity for the event service.
     * This is the maximum number of events that can be buffered
     * in the flow before backpressure is applied.
     */
    private const val BUFFER_CAPACITY: Int = 1000

    /** SharedFlow to allow broadcasting events to all active subscribers. */
    private val _eventFlow: MutableSharedFlow<String> = MutableSharedFlow(
        replay = REPLAY,
        extraBufferCapacity = BUFFER_CAPACITY
    )

    /**
     * A read-only flow of events that subscribers can collect from to receive SSE messages.
     * This flow replays the latest events for new subscribers and has extra buffer capacity to handle bursts of events.
     */
    internal val eventFlow: Flow<String> = _eventFlow

    /**
     * Pushes a new event message to the event flow
     * so that all active subscribers receive the message.
     *
     * @param message The message to push to the event flow.
     */
    public fun push(message: String) {
        runCatching {
            AsyncScope.enqueue {
                _eventFlow.emit(value = message)
            }
        }.onFailure { error ->
            tracer.error(message = "Failed to emit message to event flow.", cause = error)
        }
    }

    /**
     * Resets the event flow, clearing all replayed events.
     * This allows starting fresh with no past events available to new subscribers.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    public fun reset() {
        runCatching {
            // Reinitialize _eventFlow to clear all replayed events
            _eventFlow.resetReplayCache()
        }.onFailure { error ->
            tracer.error(message = "Failed to reset event flow.", cause = error)
        }
    }

    /**
     * Retrieves all currently replayed events from the event flow.
     * This provides a snapshot of the replay cache containing recent events.
     *
     * @return A list of all events currently stored in the replay cache.
     */
    public fun getAllEvents(): List<String> {
        return _eventFlow.replayCache
    }
}
