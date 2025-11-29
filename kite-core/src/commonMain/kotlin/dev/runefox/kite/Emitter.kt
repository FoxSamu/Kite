package dev.runefox.kite

/**
 * An emitter emits values. A [Receiver] can [subscribe] to an emitter to receive emitted values.
 */
interface Emitter<out T> {
    /**
     * Subscribes a [Receiver] to this [Emitter], causing data to flow from this emitter into the given receiver.
     */
    fun subscribe(receiver: Receiver<T>)

    // Companion object for extension functions
    companion object
}


/**
 * A listener function for when an [Emitter] opens a [Pipe].
 */
fun interface OnOpen {
    fun open(pipe: Pipe)
}

fun OnOpen(): OnOpen = IgnorantReceiver

/**
 * A listener function for when an [Emitter] emits an item.
 */
fun interface OnReceive<in T> {
    fun receive(item: T)
}

fun OnReceive(): OnReceive<Any?> = IgnorantReceiver

/**
 * A listener function for when an [Emitter] terminates a stream.
 */
fun interface OnComplete {
    fun complete()
}

fun OnComplete(): OnComplete = IgnorantReceiver

/**
 * A listener function for when an [Emitter] terminates a stream with an item.
 */
fun interface OnReceiveComplete<in T> {
    fun complete(item: T)
}

fun OnReceiveComplete(): OnReceiveComplete<Any?> = IgnorantReceiver

/**
 * A listener function for when an [Emitter] terminates a stream with an error.
 */
fun interface OnError {
    fun error(error: Throwable)
}

fun OnError(): OnError = IgnorantReceiver


/**
 * A receiver receives values. A receiver can [subscribe][Emitter.subscribe] to an [Emitter] to receive values it emits.
 *
 * **Upon subscribe, the [Emitter] will, in due time, call:**
 * 1. [open] **exactly once** (1), providing a [Pipe];
 * 2. [receive] **zero to many times** (0-N), providing emitted data;
 * 3. one of [complete] or [error] **at most once** (0-1), providing a terminal signal.
 *
 * Thus, the [Emitter] will always call the methods of a receiver in the following pattern:
 * ```text
 * open receive* (complete|error)?
 * ```
 *
 * **A [Emitter] should never:**
 * - not call [open];
 * - call [receive], [complete] or [error] before [open];
 * - call [receive], [complete] or [error] after [complete] or [error];
 * - call [open], [complete] or [error] more than once.
 *
 * A [Emitter] could choose to never call [complete] or [error]. In this case, the [Emitter] emits a never ending stream of data.
 */
interface Receiver<in T> : ManyReceiver<T>, MaybeReceiver<T>, MonoReceiver<T>, MuteReceiver {
    /**
     * Called by the [Emitter] upon subscription, providing the [Pipe] instance that can be used by the receiver to request values or close the pipe.
     */
    override fun open(pipe: Pipe)

    /**
     * Called by the [Emitter] to indicate that the next value is available.
     */
    override fun receive(item: T)

    /**
     * Called by the [Emitter] to indicate that it has emitted all values to this receiver. This signal is terminal, meaning that
     * no more calls will happen to [receive] after [complete].
     */
    override fun complete()

    /**
     * Called by the [Emitter] to indicate that it has failed to emit all values. This signal is terminal, meaning that
     * no more calls will happen to [receive] after [error].
     */
    override fun error(error: Throwable)

    /**
     * A combination of [receive] and [complete].
     */
    override fun complete(item: T) {
        receive(item)
        complete()
    }

    // Companion object for extension functions
    companion object
}

fun Receiver(): Receiver<Any?> = IgnorantReceiver

fun <T> Receiver(
    onOpen: OnOpen = OnOpen(),
    onReceive: OnReceive<T> = OnReceive(),
    onComplete: OnComplete = OnComplete(),
    onError: OnError = OnError()
): Receiver<T> = ReceiverImpl(onOpen, onReceive, onComplete, onError)


/**
 * A pipe is the connection of a [Receiver] to an [Emitter] and allows the [Receiver] to indicate its needs to the [Emitter] in a
 * decoupled fashion. [Receiver]s will never receive data if they don't [request] this.
 */
interface Pipe : AutoCloseable {
    /**
     * Requests [n] items from the [Emitter]. The [Emitter] will call [Receiver.receive] at most [n] times.
     *
     * If [n] is negative, then the calling [Receiver] indicates that it wants to receive all remaining items.
     */
    fun request(n: Long = 1L)

    /**
     * Requests all remaining items from the [Emitter].
     */
    fun requestAll() {
        request(-1)
    }

    /**
     * Requests the [Emitter] to close the pipe.
     */
    override fun close()

    // Companion object for extension functions
    companion object
}


// Implementations

/**
 * A [Receiver] with no backpressure that ignores everything it receives.
 */
internal object IgnorantReceiver : Receiver<Any?> {
    override fun open(pipe: Pipe) = pipe.requestAll()
    override fun receive(item: Any?) = Unit
    override fun complete() = Unit
    override fun complete(item: Any?) = Unit
    override fun error(error: Throwable) = Unit
}

internal class ReceiverImpl<T>(open: OnOpen, receive: OnReceive<T>, complete: OnComplete, error: OnError) :
    Receiver<T>,
    OnOpen by open,
    OnReceive<T> by receive,
    OnComplete by complete,
    OnError by error
