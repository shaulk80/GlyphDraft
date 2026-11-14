package com.glyphdraft.core

import kotlinx.coroutines.CompletableDeferred

/**
 * Turns "start something that answers later through a callback" into a suspending call. Used where a platform
 * shows its own screen and reports back separately (the Android system pickers).
 *
 * The bridge outlives the screen that shows the picker: after a rotation the new screen sets [launcher] again and
 * delivers the result to the same pending request. Call it from one thread (the UI thread).
 */
class ResultBridge<I, O : Any> {
    /** Starts the platform screen for an input. Set while a screen is attached, null otherwise. */
    var launcher: ((I) -> Outcome<Unit>)? = null

    private var pending: CompletableDeferred<O?>? = null

    /** Success with null means the user cancelled. Failure means the screen could not be started. */
    suspend fun request(input: I): Outcome<O?> {
        val start = launcher
        if (start == null) {
            return Outcome.Failure("The screen is not ready; try again in a moment.")
        }

        val deferred = CompletableDeferred<O?>()
        pending = deferred
        try {
            val started = start(input)
            if (started is Outcome.Failure) {
                return started
            }

            return Outcome.Success(deferred.await())
        } finally {
            if (pending === deferred) {
                pending = null
            }
        }
    }

    /** Called by the platform callback; null means cancelled. Ignored when nothing is waiting. */
    fun deliver(result: O?) {
        pending?.complete(result)
    }
}
