package dev.runefox.kite

import dev.runefox.kite.OnContractViolation.*

/**
 * A [Mono] produces exactly one (`1`) value.
 */
interface Mono<out T> : Emitter<T> {
    companion object
}

fun <T> Mono<T>.subscribe(
    receiver: MonoReceiver<T>,
    onViolation: OnContractViolation = IGNORE
) {
    subscribe(receiver.toReceiver(onViolation))
}

fun <T> Mono<T>.subscribe(
    onComplete: OnReceiveComplete<T> = OnReceiveComplete(),
    onError: OnError = OnError(),
    onViolation: OnContractViolation = IGNORE
) {
    subscribe(MonoReceiver(onComplete = onComplete, onError = onError), onViolation)
}

fun <T> Mono<T>.subscribe(onViolation: OnContractViolation = IGNORE) {
    subscribe(MonoReceiver(), onViolation)
}



// Receiver
// ========================================================

/**
 * A receiver that subscribes to a [Mono]. It is not directly an instance of [Receiver], but it can be converted to one using [toReceiver].
 * Instead, [Receiver] is a subtype of [MonoReceiver].
 */
interface MonoReceiver<in T> : OnOpen, OnReceiveComplete<T>, OnError {
    override fun open(pipe: Pipe) = pipe.requestAll()

    override fun complete(item: T)
    override fun error(error: Throwable)
}

/**
 * Creates a [MonoReceiver] that ignores everything.
 */
fun MonoReceiver(): MonoReceiver<Any?> {
    return Receiver()
}

/**
 * Creates a [MonoReceiver] that calls [onComplete] once it received a value, or [onError] once it receives an error.
 */
fun <T> MonoReceiver(
    onComplete: OnReceiveComplete<T> = OnReceiveComplete(),
    onError: OnError = OnError()
): MonoReceiver<T> {
    return MonoReceiverImpl(onComplete, onError)
}

fun <T> MonoReceiver<T>.toReceiver(onViolation: OnContractViolation = IGNORE): Receiver<T> {
    return MonoReceiverWrapper(this, onViolation)
}


// Implementations
// ========================================================

private class MonoReceiverImpl<T>(onComplete: OnReceiveComplete<T>, onError: OnError) :
    MonoReceiver<T>,
    OnReceiveComplete<T> by onComplete,
    OnError by onError

private class MonoReceiverWrapper<T>(
    val delegate: MonoReceiver<T>,
    val onContractViolation: OnContractViolation
) : Receiver<T> {
    private enum class MonoState {
        INIT,
        RECEIVED,
        DONE
    }

    private var state = MonoState.INIT
    private val result = OptionVar<T>()

    private fun violation(message: String) = when (onContractViolation) {
        THROW -> throw IllegalStateException(message)
        DELEGATE -> when (state) {
            MonoState.DONE -> state // We can't do anything but to ignore this exception now
            else -> {
                delegate.error(IllegalStateException(message))
                MonoState.DONE
            }
        }
        IGNORE -> state
    }

    override fun open(pipe: Pipe) {
        delegate.open(pipe)
    }

    override fun receive(item: T) {
        state = when (state) {
            MonoState.INIT -> {
                result.some(item)
                MonoState.RECEIVED
            }

            MonoState.RECEIVED -> violation("Mono received more than one value")
            MonoState.DONE -> violation("Mono received value after termination")
        }
    }

    override fun complete() {
        state = when (state) {
            MonoState.RECEIVED -> {
                delegate.complete(result.value())
                MonoState.DONE
            }

            MonoState.INIT -> violation("Mono received completion before any value")
            MonoState.DONE -> violation("Mono received second termination")
        }
    }

    override fun error(error: Throwable) {
        state = when (state) {
            MonoState.RECEIVED, MonoState.INIT -> {
                delegate.error(error)
                MonoState.DONE
            }

            MonoState.DONE -> violation("Mono received second termination")
        }
    }
}
