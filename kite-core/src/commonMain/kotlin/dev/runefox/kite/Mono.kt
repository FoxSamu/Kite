package dev.runefox.kite

/**
 * A [Mono] produces exactly one (`1`) value.
 */
interface Mono<out T> : Emitter<T> {
    companion object
}

interface MonoReceiver<in T> {
    fun open(pipe: Pipe)
    fun complete(value: T)
    fun error(error: Throwable)
}

private class MonoReceiverWrapper<T>(val delegate: MonoReceiver<T>) : Receiver<T> {
    private var received = false
    private var terminated = false

    override fun open(pipe: Pipe) {
        delegate.open(pipe)
    }

    override fun receive(item: T) {
        if (terminated) {
            return
        }

        if (received) {
            return error(IllegalStateException("Mono generated two values!"))
        }


        received = true
    }

    override fun complete() {
        TODO("Not yet implemented")
    }

    override fun error(error: Throwable) {
        TODO("Not yet implemented")
    }

}