package dev.runefox.kite

import dev.runefox.kite.OnContractViolation.*

/**
 * A [Maybe] produces zero or one (`0..1`) value.
 */
interface Maybe<out T> : Emitter<T> {
    companion object
}

fun <T> Maybe<T>.subscribe(
    receiver: MaybeReceiver<T>,
    onViolation: OnContractViolation = IGNORE
) {
    subscribe(receiver.toReceiver(onViolation))
}

fun <T> Maybe<T>.subscribe(
    onPresent: OnReceiveComplete<T> = OnReceiveComplete(),
    onAbsent: OnComplete = OnComplete(),
    onError: OnError = OnError(),
    onViolation: OnContractViolation = IGNORE
) {
    subscribe(MaybeReceiver(onPresent = onPresent, onAbsent = onAbsent, onError = onError), onViolation)
}

fun <T> Maybe<T>.subscribe(onViolation: OnContractViolation = IGNORE) {
    subscribe(MaybeReceiver(), onViolation)
}



// Receiver
// ========================================================

/**
 * A receiver that subscribes to a [Maybe]. It is not directly an instance of [Receiver], but it can be converted to one using [toReceiver].
 * Instead, [Receiver] is a subtype of [MaybeReceiver].
 */
interface MaybeReceiver<in T> : OnOpen, OnReceiveComplete<T>, OnComplete, OnError {
    override fun open(pipe: Pipe) = pipe.requestAll()

    override fun complete(item: T)
    override fun complete()
    override fun error(error: Throwable)
}

/**
 * Creates a [MaybeReceiver] that ignores everything.
 */
fun MaybeReceiver(): MaybeReceiver<Any?> {
    return Receiver()
}

/**
 * Creates a [MaybeReceiver] that calls [onPresent] once it received a value, [onAbsent] once it receives an empty completion,
 * or [onError] once it receives an error.
 */
fun <T> MaybeReceiver(
    onPresent: OnReceiveComplete<T> = OnReceiveComplete(),
    onAbsent: OnComplete = OnComplete(),
    onError: OnError = OnError()
): MaybeReceiver<T> {
    return MaybeReceiverImpl(onPresent, onAbsent, onError)
}

fun <T> MaybeReceiver<T>.toReceiver(onViolation: OnContractViolation = IGNORE): Receiver<T> {
    return MaybeReceiverWrapper(this, onViolation)
}



// Implementations
// ========================================================

private class MaybeReceiverImpl<T>(onPresent: OnReceiveComplete<T>, onAbsent: OnComplete, onError: OnError) :
    MaybeReceiver<T>,
    OnReceiveComplete<T> by onPresent,
    OnComplete by onAbsent,
    OnError by onError

private class MaybeReceiverWrapper<T>(
    val delegate: MaybeReceiver<T>,
    val onContractViolation: OnContractViolation
) : Receiver<T> {
    private enum class MaybeState {
        INIT,
        RECEIVED,
        DONE
    }

    private var state = MaybeState.INIT
    private val result = OptionVar<T>()

    private fun violation(message: String) = when (onContractViolation) {
        THROW -> throw IllegalStateException(message)
        DELEGATE -> when (state) {
            MaybeState.DONE -> state // We can't do anything but to ignore this exception now
            else -> {
                delegate.error(IllegalStateException(message))
                MaybeState.DONE
            }
        }
        IGNORE -> state
    }

    override fun open(pipe: Pipe) {
        delegate.open(pipe)
    }

    override fun receive(item: T) {
        state = when (state) {
            MaybeState.INIT -> {
                result.some(item)
                MaybeState.RECEIVED
            }

            MaybeState.RECEIVED -> violation("Maybe received more than one value")
            MaybeState.DONE -> violation("Maybe received value after termination")
        }
    }

    override fun complete() {
        state = when (state) {
            MaybeState.RECEIVED -> {
                delegate.complete()
                MaybeState.DONE
            }

            MaybeState.INIT -> violation("Maybe received completion before any value")
            MaybeState.DONE -> violation("Maybe received second termination")
        }
    }

    override fun error(error: Throwable) {
        state = when (state) {
            MaybeState.RECEIVED, MaybeState.INIT -> {
                delegate.error(error)
                MaybeState.DONE
            }

            MaybeState.DONE -> violation("Maybe received second termination")
        }
    }
}
