package dev.runefox.kite

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/*
 * GENERATE
 * --------
 *
 * The GENERATE operator creates a publisher that has generates values by means of a coroutine. The coroutine receives an Output instance, to which
 * it can emit values. The emit call will suspend until these values are requested, and will throw a CancellationException when the pipe is closed.
 *
 * E.g. the following will generate a Many that emit each line entered to the standard input:
 *
 * Many.generate {
 *     while (true) {
 *         emit(readlnOrNull() ?: return)
 *     }
 * }
 */

fun interface Output<in T> {
    suspend fun emit(item: T)
}

suspend fun <T> Output<T>.emitAll(iterable: Sequence<T>) {
    for (item in iterable) {
        emit(item)
    }
}

suspend fun <T> Output<T>.emitAll(iterable: Iterable<T>) {
    for (item in iterable) {
        emit(item)
    }
}

suspend fun <T> Output<T>.emitAll(iterable: Iterator<T>) {
    for (item in iterable) {
        emit(item)
    }
}

suspend fun <T> Output<T>.emitAll(iterable: Array<T>) {
    for (item in iterable) {
        emit(item)
    }
}

suspend fun <K, V> Output<Map.Entry<K, V>>.emitAll(iterable: Map<K, V>) {
    for (item in iterable) {
        emit(item)
    }
}

private class CoroutineGenerator<T>(
    dst: Receiver<T>,
    context: CoroutineContext,
    generate: suspend Output<T>.() -> Unit
) : Generator<T>(dst) {
    private val runner = CoroutineRunner(
        context,
        { yield -> Output<T> { emitSuspend(it, yield) }.generate() },
        { emitComplete() },
        { emitError(it) }
    )

    private val count = RequestCount()

    override fun request(n: Long) {
        if (closed || n == 0L) {
            return
        }

        count.request(n)
        runner.resumeIfYielded()
    }

    override fun close() {
        super.close()

        // Cancel coroutine
        runner.cancelIfYielded("Pipe closed")
    }

    private suspend fun emitSuspend(item: T, yield: suspend () -> Unit) {
        // Yield until a request has been made
        while (!count.has()) {
            yield()
        }

        // Emit item
        count.remove()
        emit(item) ?: throw CancellationException("Pipe closed")
    }
}

private class MonoMaybeOutput<T>(val into: Output<T>) : Output<T> {
    private var emitted = 0

    override suspend fun emit(item: T) {
        if (emitted >= 1) {
            throw IllegalStateException("`generate` block must not call `emit` multiple times.")
        }

        emitted ++
        into.emit(item)
    }

    fun ensureMono() {
        if (emitted == 0) {
            throw IllegalStateException("`generate` block must call `emit`.")
        }
    }
}

fun <T> Mono.Companion.generate(context: CoroutineContext = EmptyCoroutineContext, block: suspend Output<T>.() -> Unit): Mono<T> {
    return generator {
        CoroutineGenerator(it, context) {
            MonoMaybeOutput(this).apply { block() }.ensureMono()
        }
    }
}

fun <T> Maybe.Companion.generate(context: CoroutineContext = EmptyCoroutineContext, block: suspend Output<T>.() -> Unit): Maybe<T> {
    return generator {
        CoroutineGenerator(it, context) {
            MonoMaybeOutput(this).apply { block() }
        }
    }
}

fun <T> Many.Companion.generate(context: CoroutineContext = EmptyCoroutineContext, block: suspend Output<T>.() -> Unit): Many<T> {
    return generator {
        CoroutineGenerator(it, context, block)
    }
}
