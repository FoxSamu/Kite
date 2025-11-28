package dev.runefox.kite

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

internal class RequestCount {
    private var count: Long = 0L

    fun get(): Long {
        return count
    }

    fun infinite(): Boolean {
        return count < 0
    }

    fun check(n: Long): Long {
        val n = max(n, 0L)

        return when (count) {
            0L -> 0L
            !in 0L..n -> n
            else -> count
        }
    }

    fun has(n: Long): Boolean {
        return check(n) == n
    }

    fun has(): Boolean {
        return has(1L)
    }

    fun remove(n: Long): Long {
        val rem = check(n)

        if (count > 0L) {
            count -= rem
        }

        return rem
    }

    fun remove(): Boolean {
        return remove(1L) == 1L
    }

    fun request(n: Long) {
        if (n < 0) {
            count = -1L
        } else {
            count += n
        }
    }
}


/**
 * A simple coroutine runner.
 */
internal class CoroutineRunner<T>(
    context: CoroutineContext,
    coroutine: suspend (yield: suspend () -> Unit) -> T,
    onReturn: (T) -> Unit,
    onThrow: (Throwable) -> Unit
) {
    private var coroutine: Continuation<Unit>? = suspend {
        coroutine { yield() }
    }.createCoroutine(object : Continuation<T> {
        override val context = context
        override fun resumeWith(result: Result<T>) = result.fold(onReturn, onThrow)
    })

    private suspend fun yield() {
        suspendCoroutine {
            if (coroutine != null) {
                it.resumeWithException(IllegalStateException("Already yielded!"))
            } else {
                coroutine = it
            }
        }
    }

    private fun resume(result: Result<Unit>) {
        val y = coroutine ?: throw IllegalStateException("Can't resume running coroutine")
        coroutine = null

        y.resumeWith(result)
    }

    private fun resumeIfYielded(result: Result<Unit>) {
        val y = coroutine ?: return
        coroutine = null

        y.resumeWith(result)
    }

    fun resume() {
        resume(RESUME)
    }

    fun resumeIfYielded() {
        resumeIfYielded(RESUME)
    }

    fun raise(exception: Throwable) {
        resume(Result.failure(exception))
    }

    fun raiseIfYielded(exception: Throwable) {
        resumeIfYielded(Result.failure(exception))
    }

    fun cancel() {
        resume(CANCEL)
    }

    fun cancelIfYielded() {
        resumeIfYielded(CANCEL)
    }

    fun cancel(message: String) {
        resume(Result.failure(CancellationException(message)))
    }

    fun cancelIfYielded(message: String) {
        resumeIfYielded(Result.failure(CancellationException(message)))
    }

    companion object {
        private val RESUME = Result.success(Unit)
        private val CANCEL = Result.failure<Unit>(CancellationException())
    }
}


internal class MaybeVar<T> {
    private var present = false
    private var value: T? = null

    fun set(value: T) {
        this.value = value
        this.present = true
    }

    fun unset() {
        this.present = false
    }

    fun has(): Boolean {
        return present
    }

    fun require(): T {
        if (has()) {
            return value as T
        } else {
            throw NoSuchElementException()
        }
    }

    fun get(): T? {
        return value
    }
}