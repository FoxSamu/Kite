package dev.runefox.kite

import dev.runefox.kite.OnContractViolation.*

/**
 * A [Many] produces zero to many (`0..N`) values.
 */
interface Many<out T> : Emitter<T> {
    companion object
}

fun <T> Many<T>.subscribe(
    receiver: ManyReceiver<T>,
    onViolation: OnContractViolation = IGNORE
) {
    subscribe(receiver.toReceiver(onViolation))
}

fun <T> Many<T>.subscribe(
    onReceive: OnReceive<T> = OnReceive(),
    onComplete: OnComplete = OnComplete(),
    onError: OnError = OnError(),
    onViolation: OnContractViolation = IGNORE
) {
    subscribe(ManyReceiver(onReceive = onReceive, onComplete = onComplete, onError = onError), onViolation)
}

fun <T> Many<T>.subscribe(onViolation: OnContractViolation = IGNORE) {
    subscribe(ManyReceiver(), onViolation)
}



// Receiver
// ========================================================

/**
 * A receiver that subscribes to a [Many]. It is not directly an instance of [Receiver], but it can be converted to one using [toReceiver].
 * Instead, [Receiver] is an subtype of [ManyReceiver].
 */
interface ManyReceiver<in T> : OnOpen, OnReceive<T>, OnComplete, OnError {
    override fun open(pipe: Pipe) = pipe.requestAll()

    override fun receive(item: T)
    override fun complete()
    override fun error(error: Throwable)
}

/**
 * Creates a [ManyReceiver] that ignores everything.
 */
fun ManyReceiver(): ManyReceiver<Any?> {
    return Receiver()
}

/**
 * Creates a [ManyReceiver] that calls [onReceive] each time it receives a value, [onComplete] once it completes,
 * and [onError] once it receives an error.
 */
fun <T> ManyReceiver(
    onReceive: OnReceive<T> = OnReceive(),
    onComplete: OnComplete = OnComplete(),
    onError: OnError = OnError()
): ManyReceiver<T> {
    return ManyReceiverImpl(onReceive, onComplete, onError)
}

fun <T> ManyReceiver<T>.toReceiver(onViolation: OnContractViolation = IGNORE): Receiver<T> {
    return ManyReceiverWrapper(this, onViolation)
}



// Implementations
// ========================================================

private class ManyReceiverImpl<T>(onReceive: OnReceive<T>, onComplete: OnComplete, onError: OnError) :
    ManyReceiver<T>,
    OnReceive<T> by onReceive,
    OnComplete by onComplete,
    OnError by onError

private class ManyReceiverWrapper<T>(
    val delegate: ManyReceiver<T>,
    val onContractViolation: OnContractViolation
) : Receiver<T>, OnOpen by delegate {
    private enum class ManyState {
        BUSY,
        DONE
    }

    private var state = ManyState.BUSY

    private fun violation(message: String) = when (onContractViolation) {
        THROW -> throw IllegalStateException(message)
        DELEGATE -> when (state) {
            ManyState.DONE -> state // We can't do anything but to ignore this exception now
            else -> {
                delegate.error(IllegalStateException(message))
                ManyState.DONE
            }
        }

        IGNORE -> state
    }

    override fun receive(item: T) {
        state = when (state) {
            ManyState.BUSY -> {
                delegate.receive(item)
                ManyState.BUSY
            }

            ManyState.DONE -> violation("Many received value after termination")
        }
    }

    override fun complete() {
        state = when (state) {
            ManyState.BUSY -> {
                delegate.complete()
                ManyState.DONE
            }

            ManyState.DONE -> violation("Many received second termination")
        }
    }

    override fun error(error: Throwable) {
        state = when (state) {
            ManyState.BUSY -> {
                delegate.error(error)
                ManyState.DONE
            }

            ManyState.DONE -> violation("Many received second termination")
        }
    }
}

