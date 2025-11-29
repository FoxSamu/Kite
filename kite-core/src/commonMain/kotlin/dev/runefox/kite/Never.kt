@file:Suppress("UNCHECKED_CAST")

package dev.runefox.kite

/*
 * NEVER
 * -----
 *
 * The NEVER operator creates a publisher that never produces a value, never completes, and never fails.
 */

private object NeverPipe : Pipe {
    override fun request(n: Long) {
        // N/A
    }

    override fun close() {
        // N/A
    }
}

private object Never : Many<Nothing>, Maybe<Nothing>, Mono<Nothing>, Mute, Emitter<Nothing> {
    override fun subscribe(receiver: Receiver<Nothing>) {
        receiver.open(NeverPipe)
    }
}

fun Mute.Companion.never(): Mute {
    return Never
}

fun <T> Mono.Companion.never(): Mono<T> {
    return Never
}

fun <T> Maybe.Companion.never(): Maybe<T> {
    return Never
}

fun <T> Many.Companion.never(): Many<T> {
    return Never
}
