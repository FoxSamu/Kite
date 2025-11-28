package dev.runefox.kite

/**
 * A special value used internally to represent a complete signal.
 */
internal object CompleteSignal

/**
 * A special value used internally to represent an error signal.
 */
internal class ErrorSignal(val error: Throwable)


internal interface Destination<T> : Pipe {
    val closed: Boolean
    val dst: Receiver<T>

    /**
     * Emits a value. If the pipe was closed after emission, this returns null so this null can cascade easily by using `?: return null`.
     * If the pipe was still open after emission, [Unit] is returned.
     */
    fun emit(item: T): Unit? {
        if (!closed) {
            dst.receive(item)
        }

        return if (closed) null else Unit
    }

    /**
     * Emits a completion. If the pipe was closed after emission, this returns null so this null can cascade easily by using `?: return null`.
     * Completion always closes the pipe, so this method will always return null.
     */
    fun emitComplete(): Nothing? {
        if (!closed) {
            close()
            dst.complete()
        }

        return null
    }

    /**
     * Emits an error. If the pipe was closed after emission, this returns null so this null can cascade easily by using `?: return null`.
     * Error always closes the pipe, so this method will always return null.
     */
    fun emitError(error: Throwable): Nothing? {
        if (!closed) {
            close()
            dst.error(error)
        }

        return null
    }
}

internal interface Source<I> : Receiver<I> {
    val closed: Boolean
    val src: Pipe

    fun take(n: Long = 1): Unit? {
        if (!closed) {
            src.request(n)
        }

        return if (closed) null else Unit
    }

    fun takeAll(n: Long = 1): Unit? {
        if (!closed) {
            src.requestAll()
        }

        return if (closed) null else Unit
    }
}


/**
 * An operator sits inbetween a [Receiver] and a [Pipe], transforming requests from the [Receiver] to the [Pipe].
 * An operator acts as a [Pipe] to a [Receiver] and as a [Receiver] to a [Pipe].
 */
internal abstract class Operator<I, O>(
    final override val dst: Receiver<O>
) : Source<I>, Destination<O> {
    final override lateinit var src: Pipe
        private set

    final override var closed = false
        private set

    override fun open(pipe: Pipe) {
        dst.open(this)
        src = pipe
    }

    override fun complete() {
        emitComplete()
    }

    override fun error(error: Throwable) {
        emitError(error)
    }

    override fun request(n: Long) {
        take(n)
    }

    override fun close() {
        src.close()
        closed = true
    }
}


/**
 * A generator sits in front of a [Receiver], generating elements to it. It acts as a [Pipe] to a [Receiver].
 */
internal abstract class Generator<O>(
    final override val dst: Receiver<O>
) : Destination<O> {
    final override var closed = false
        private set

    open fun open() {
        dst.open(this)
    }

    override fun close() {
        closed = true
    }
}

internal class EmptyGenerator<O>(
    receiver: Receiver<O>
) : Generator<O>(receiver) {
    override fun request(n: Long) {
        emitComplete()
    }
}

internal class SingleGenerator<O>(
    receiver: Receiver<O>,
    val value: O
) : Generator<O>(receiver) {
    override fun request(n: Long) {
        if (n != 0L) {
            emit(value) ?: return
            emitComplete()
        }
    }
}

internal class IterableGenerator<T>(
    receiver: Receiver<T>,
    val value: Iterator<T>
) : Generator<T>(receiver) {
    private fun checkNext(): Unit? {
        val hasNext = try {
            value.hasNext()
        } catch (e: Throwable) {
            return emitError(e)
        }

        if (!hasNext) {
            return emitComplete()
        }

        return Unit
    }

    private fun emitNext(): Unit? {
        checkNext() ?: return null

        val value = try {
            value.next()
        } catch (e: Throwable) {
            return emitError(e)
        }

        emit(value) ?: return null

        return Unit
    }

    override fun request(n: Long) {
        var i = 0L

        while (n !in 0..i) {
            emitNext() ?: return
            i ++
        }

        checkNext()
    }
}

internal inline fun <T> Mono.Companion.generator(crossinline generator: (Receiver<T>) -> Generator<T>) = object : Mono<T> {
    override fun subscribe(receiver: Receiver<T>) {
        generator(receiver).open()
    }
}

internal inline fun <T> Maybe.Companion.generator(crossinline generator: (Receiver<T>) -> Generator<T>) = object : Maybe<T> {
    override fun subscribe(receiver: Receiver<T>) {
        generator(receiver).open()
    }
}

internal inline fun <T> Many.Companion.generator(crossinline generator: (Receiver<T>) -> Generator<T>) = object : Many<T> {
    override fun subscribe(receiver: Receiver<T>) {
        generator(receiver).open()
    }
}

internal inline fun <I, O> Mono<I>.operator(crossinline operator: (Receiver<O>) -> Operator<I, O>) = object : Mono<O> {
    override fun subscribe(receiver: Receiver<O>) {
        this@operator.subscribe(operator(receiver))
    }
}

internal inline fun <I, O> Maybe<I>.operator(crossinline operator: (Receiver<O>) -> Operator<I, O>) = object : Maybe<O> {
    override fun subscribe(receiver: Receiver<O>) {
        this@operator.subscribe(operator(receiver))
    }
}

internal inline fun <I, O> Many<I>.operator(crossinline operator: (Receiver<O>) -> Operator<I, O>) = object : Many<O> {
    override fun subscribe(receiver: Receiver<O>) {
        this@operator.subscribe(operator(receiver))
    }
}