/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.event

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import krud.base.env.Tracer

/**
 * An object that manages asynchronous actions.
 *
 * This object provides a mechanism to enqueue suspending actions that can be executed
 * either in parallel (default) or sequentially, depending on the configuration.
 *
 * #### Attention
 * All actions enqueued before shutdown are guaranteed to be executed.
 * During shutdown, the application will wait for all pending actions to complete.
 */
public object AsyncScope {
    private val tracer: Tracer = Tracer<AsyncScope>()

    /** The default capacity of the task channel. */
    private const val DEFAULT_CAPACITY: Int = 128

    /**
     * Configuration for parallel or sequential task processing.
     * By default, tasks are executed in parallel.
     */
    @Volatile
    public var isParallel: Boolean = true

    /**
     * The coroutine scope used for launching the action processing coroutine.
     */
    private var scope: CoroutineScope = CoroutineScope(context = SupervisorJob() + Dispatchers.Default)

    /**
     * Sets the scope to be used for coroutine operations,
     * allowing to override the internal default one.
     *
     * Useful to tie the scope to a concrete lifecycle, such as an application
     * or a specific component, ensuring proper cancellation and resource management.
     *
     * @param scope The coroutine scope to assign.
     */
    public fun setScope(scope: CoroutineScope) {
        this.scope = scope
    }

    /**
     * The channel that holds the suspending actions to be executed sequentially if needed.
     */
    private val taskChannel: Channel<suspend () -> Unit> = Channel(
        capacity = DEFAULT_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Job that handles sequential task processing, started only if `isParallel` is false.
     */
    private val sequentialJob: Job = scope.launch {
        for (action in taskChannel) {
            try {
                action() // Executes each action sequentially.
            } catch (e: Throwable) {
                tracer.error(message = "Error executing async action.", cause = e)
            }
        }
    }

    /**
     * Enqueues a new suspending action to be executed.
     *
     * The action will be executed either in parallel (default) or sequentially based on
     * the value of [AsyncScope.isParallel].
     *
     * @param action The suspending action to enqueue.
     * @throws Exception if the action cannot be enqueued because the channel is closed.
     */
    public fun enqueue(action: suspend () -> Unit) {
        if (isParallel) {
            // Parallel execution: Launch each action as a separate coroutine.
            scope.launch {
                try {
                    action()
                } catch (e: Throwable) {
                    tracer.error(message = "Error executing async action.", cause = e)
                }
            }
        } else {
            // Sequential execution: Send the action to the task channel.
            scope.launch {
                try {
                    taskChannel.send(element = action)
                } catch (e: ClosedSendChannelException) {
                    tracer.error(message = "Async action channel is closed.", cause = e)
                }
            }
        }
    }

    /**
     * Closes the action channel and cancels the coroutine scope.
     *
     * This should be called when the application is shutting down to ensure that
     * all resources are properly released and that no new actions can be enqueued.
     *
     * @return A [Job] that can be awaited to ensure all pending actions are completed.
     */
    public fun close(): Job {
        if (!isParallel) {
            // Closes the channel if using sequential mode.
            taskChannel.close()
        }
        // Cancel the scope to stop all running coroutines.
        return sequentialJob
    }
}
