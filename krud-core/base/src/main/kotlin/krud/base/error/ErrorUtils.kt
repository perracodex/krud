/*
 * Copyright (c) 2024-Present Perracodex. Use of this source code is governed by an MIT license.
 */

package krud.base.error

/**
 * Provides utility functions to assist with error handling.
 */
internal object ErrorUtils {
    /**
     * Builds a detailed error message by extracting unique messages from the chain of causes
     * of the provided [cause], up to a predefined number of initial error points that are most
     * relevant for diagnostics.
     *
     * @param cause The initial [Throwable] from which to start extracting the messages.
     * @return A detailed error message string, comprising unique messages, up to the predefined limit.
     */
    fun summarizeCause(cause: Throwable): String {
        // Use a Set to keep track of unique messages.
        val uniqueMessages: LinkedHashSet<String> = linkedSetOf()
        // Maximum number of unique messages to collect.
        val maxMessages = 2
        // Current throwable in the chain.
        var currentCause: Throwable? = cause

        // Iterate through the exception chain and collect unique messages until we reach the limit.
        while (uniqueMessages.size < maxMessages && currentCause != null) {
            // Add the message if it is unique.
            currentCause.message?.let { message ->
                uniqueMessages.add(message)
            }
            // Move to the next cause in the chain.
            currentCause = currentCause.cause
        }

        // Join the collected messages with "Caused by" if more than one, otherwise return the single message.
        return uniqueMessages.joinToString(separator = " Caused by: ")
    }
}
