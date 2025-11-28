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

private object Never : Many<Any?>, Maybe<Any?>, Mono<Any?>, Emitter<Any?> {
    override fun subscribe(receiver: Receiver<Any?>) {
        receiver.open(NeverPipe)
    }
}

fun <T> Mono.Companion.never(): Mono<T> {
    return Never as Mono<T>
}

fun <T> Maybe.Companion.never(): Maybe<T> {
    return Never as Maybe<T>
}

fun <T> Many.Companion.never(): Many<T> {
    return Never as Many<T>
}
