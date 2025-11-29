package dev.runefox.kite

import dev.runefox.kite.OnContractViolation.*

/**
 * A [Mute] produces zero (`0`) values. It only produces a completion signal or an error signal.
 */
interface Mute : Emitter<Nothing> {
    companion object
}

fun Mute.subscribe(
    receiver: MuteReceiver,
    onViolation: OnContractViolation = IGNORE
) {
    subscribe(receiver.toReceiver(onViolation))
}

fun Mute.subscribe(
    onComplete: OnComplete = OnComplete(),
    onError: OnError = OnError(),
    onViolation: OnContractViolation = IGNORE
) {
    subscribe(MuteReceiver(onComplete = onComplete, onError = onError), onViolation)
}

fun Mute.subscribe(onViolation: OnContractViolation = IGNORE) {
    subscribe(MuteReceiver(), onViolation)
}



// Receiver
// ========================================================

/**
 * A receiver that subscribes to a [Mute]. It is not directly an instance of [Receiver], but it can be converted to one using [toReceiver].
 * Instead, [Receiver] is a subtype of [MuteReceiver].
 */
interface MuteReceiver : OnOpen, OnComplete, OnError {
    override fun open(pipe: Pipe) = pipe.requestAll()

    override fun complete()
    override fun error(error: Throwable)
}

/**
 * Creates a [MuteReceiver] that ignores everything.
 */
fun MuteReceiver(): MuteReceiver {
    return Receiver()
}

/**
 * Creates a [MuteReceiver] that calls [onComplete] once it completes, or [onError] once it receives an error.
 */
fun MuteReceiver(
    onComplete: OnComplete = OnComplete(),
    onError: OnError = OnError()
): MuteReceiver {
    return MuteReceiverImpl(onComplete, onError)
}

fun MuteReceiver.toReceiver(onViolation: OnContractViolation = IGNORE): Receiver<Any?> {
    return MuteReceiverWrapper(this, onViolation)
}


// Implementations
// ========================================================

private class MuteReceiverImpl(onComplete: OnComplete, onError: OnError) :
    MuteReceiver,
    OnComplete by onComplete,
    OnError by onError

private class MuteReceiverWrapper(
    val delegate: MuteReceiver,
    val onContractViolation: OnContractViolation
) : Receiver<Any?> {
    private enum class MuteState {
        INIT,
        DONE
    }

    private var state = MuteState.INIT

    private fun violation(message: String) = when (onContractViolation) {
        THROW -> throw IllegalStateException(message)
        DELEGATE -> when (state) {
            MuteState.DONE -> state // We can't do anything but to ignore this exception now
            else -> {
                delegate.error(IllegalStateException(message))
                MuteState.DONE
            }
        }
        IGNORE -> state
    }

    override fun open(pipe: Pipe) {
        delegate.open(pipe)
    }

    override fun receive(item: Any?) {
        state = violation("Mute received value")
    }

    override fun complete() {
        state = when (state) {
            MuteState.INIT -> {
                delegate.complete()
                MuteState.DONE
            }

            MuteState.DONE -> violation("Mono received second termination")
        }
    }

    override fun error(error: Throwable) {
        state = when (state) {
            MuteState.INIT -> {
                delegate.error(error)
                MuteState.DONE
            }

            MuteState.DONE -> violation("Mono received second termination")
        }
    }
}
