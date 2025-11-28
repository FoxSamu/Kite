package dev.runefox.kite

internal abstract class AbstractBuffer<T>(
    receiver: Receiver<T>
) : Operator<T, T>(receiver) {
    protected val buffer = ArrayDeque<Any?>()
    protected val count = RequestCount()

    private fun signal(signal: Any?) {
        if (closed) {
            return
        }

        buffer.addLast(signal)
        drain()
    }

    @Suppress("UNCHECKED_CAST")
    private fun drain() {
        while (count.has() && buffer.isNotEmpty() && !closed) {
            val signal = buffer.removeFirst()
            count.remove()

            when (signal) {
                is CompleteSignal -> dst.complete()
                is ErrorSignal -> dst.error(signal.error)
                else -> dst.receive(signal as T)
            }
        }
    }

    override fun receive(item: T) {
        signal(item)
    }

    override fun complete() {
        signal(CompleteSignal)
    }

    override fun error(error: Throwable) {
        signal(ErrorSignal(error))
    }

    override fun request(n: Long) {
        if (!closed) {
            count.request(n)
            drain()
        }
    }

    override fun close() {
        super.close()
        buffer.clear()
    }
}


internal class LimitedBuffer<T>(
    receiver: Receiver<T>,
    val maxCapacity: Int
) : AbstractBuffer<T>(receiver) {
    private lateinit var source: Pipe

    override fun open(pipe: Pipe) {
        super.open(pipe)
        request(maxCapacity.toLong())
    }

    override fun request(n: Long) {
        super.request(n)

        if (!count.has()) {
            return
        }

        if (count.infinite()) {
            source.requestAll()
        } else {
            // If we still have buffer space, request some extra
            val requestExtra = (maxCapacity - buffer.size).toLong()

            source.request(count.get() + requestExtra)
        }
    }
}


internal class EndlessBuffer<T>(
    receiver: Receiver<T>
) : AbstractBuffer<T>(receiver) {
    override fun open(pipe: Pipe) {
        super.open(pipe)
        requestAll()
    }
}
