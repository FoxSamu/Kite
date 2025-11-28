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
interface Receiver<in T> {
    /**
     * Called by the [Emitter] upon subscription, providing the [Pipe] instance that can be used by the receiver to request values or close the pipe.
     */
    fun open(pipe: Pipe)

    /**
     * Called by the [Emitter] to indicate that the next value is available.
     */
    fun receive(item: T)

    /**
     * Called by the [Emitter] to indicate that it has emitted all values to this receiver. This signal is terminal, meaning that
     * no more calls will happen to [receive] after [complete].
     */
    fun complete()

    /**
     * Called by the [Emitter] to indicate that it has failed to emit all values. This signal is terminal, meaning that
     * no more calls will happen to [receive] after [error].
     */
    fun error(error: Throwable)

    // Companion object for extension functions
    companion object
}


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