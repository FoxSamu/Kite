package dev.runefox.kite

// Does not need to implement Mute or Mono because they can not be escalated to, only from
private class EscalatedEmitter<T>(delegate: Emitter<T>) : Many<T>, Maybe<T>, Emitter<T> by delegate

fun Mute.asMaybe(): Maybe<Nothing> {
    return EscalatedEmitter(this)
}

fun Mute.asMany(): Many<Nothing> {
    return EscalatedEmitter(this)
}

fun <T> Mono<T>.asMaybe(): Maybe<T> {
    return EscalatedEmitter(this)
}

fun <T> Mono<T>.asMany(): Many<T> {
    return EscalatedEmitter(this)
}

fun <T> Maybe<T>.asMany(): Many<T> {
    return EscalatedEmitter(this)
}



private class MuteMono<T>(val mute: Mute, val completer: () -> T) : Mono<T> {
    override fun subscribe(receiver: Receiver<T>) {
        mute.subscribe(Delegate(receiver))
    }

    private inner class Delegate(val receiver: Receiver<T>) : MuteReceiver {
        override fun open(pipe: Pipe) {
            receiver.open(pipe)
        }

        override fun complete() {
            receiver.complete(completer())
        }

        override fun error(error: Throwable) {
            receiver.error(error)
        }
    }
}

fun <T> Mute.asMono(completer: () -> T): Mono<T> {
    return MuteMono(this, completer)
}

fun Mute.asUnitMono(): Mono<Unit> {
    return asMono { }
}

fun Mute.asNullMono(): Mono<Nothing?> {
    return asMono { null }
}



private class MaybeMono<T, U>(val maybe: Maybe<T>, val present: (T) -> U, val absent: () -> U) : Mono<U> {
    override fun subscribe(receiver: Receiver<U>) {
        maybe.subscribe(Delegate(receiver))
    }

    private inner class Delegate(val receiver: Receiver<U>) : MaybeReceiver<T> {
        override fun open(pipe: Pipe) {
            receiver.open(pipe)
        }

        override fun complete(item: T) {
            receiver.complete(present(item))
        }

        override fun complete() {
            receiver.complete(absent())
        }

        override fun error(error: Throwable) {
            receiver.error(error)
        }
    }
}

fun <T> Maybe<T>.asMono(absentProvider: () -> T): Mono<T> {
    return MaybeMono(this, { it }, absentProvider)
}

fun <T> Maybe<T>.asOptionMono(): Mono<Option<T>> {
    return MaybeMono(this, { Option.some(it) }, { Option.none })
}

fun <T> Maybe<T>.asNullableMono(): Mono<T?> {
    return MaybeMono(this, { it }, { null })
}


private class Muter(val mono: Emitter<*>) : Mute {
    override fun subscribe(receiver: Receiver<Nothing>) {
        mono.subscribe(Delegate(receiver))
    }

    private class Delegate(val receiver: Receiver<Nothing>) : Receiver<Any?> {
        override fun open(pipe: Pipe) {
            receiver.open(pipe)
        }

        override fun receive(item: Any?) = Unit

        override fun complete() {
            receiver.complete()
        }

        override fun error(error: Throwable) {
            receiver.error(error)
        }
    }
}

fun <T> Emitter<T>.mute(): Mute {
    return Muter(this)
}
