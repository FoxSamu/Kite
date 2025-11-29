package dev.runefox.kite

/*
 * JUST
 * ----
 *
 * The JUST operator creates a publisher that has has a predefined sequence of values that it publishes to subscribers.
 *
 * E.g.: Mono.just(3) returns a Mono that will publish 3 to all subscribers.
 */


/**
 * Creates a [Mute] that always immediately completes.
 */
fun Mute.Companion.just(): Mute {
    return generator { EmptyGenerator(it) }
}


/**
 * Creates a [Mono] that always immediately publishes [value].
 */
fun <T> Mono.Companion.just(value: T): Mono<T> {
    return generator { SingleGenerator(it, value) }
}


/**
 * Creates a [Maybe] that always immediately completes without value.
 */
fun Maybe.Companion.just(): Maybe<Nothing> {
    return generator { EmptyGenerator(it) }
}

/**
 * Creates a [Maybe] that always immediately publishes [value].
 */
fun <T> Maybe.Companion.just(value: T): Maybe<T> {
    return generator { SingleGenerator(it, value) }
}


/**
 * Creates a [Many] that always immediately completes without any values.
 */
fun Many.Companion.just(): Many<Nothing> {
    return generator { EmptyGenerator(it) }
}

/**
 * Creates a [Many] that always immediately publishes [value] and then completes.
 */
fun <T> Many.Companion.just(value: T): Many<T> {
    return generator { SingleGenerator(it, value) }
}

/**
 * Creates a [Many] that always immediately publishes the given [values] and then completes.
 */
fun <T> Many.Companion.just(vararg values: T): Many<T> {
    return when (values.size) {
        0 -> just()
        1 -> just(values[0])
        else -> generator { IterableGenerator(it, values.iterator()) }
    }
}

/**
 * Creates a [Many] that always immediately publishes the given [values] and then completes.
 */
fun <T> Many.Companion.just(values: Iterable<T>): Many<T> {
    if (values is List<T> && values.size < 2) {
        return when (values.size) {
            0 -> just()
            else -> just(values[0])
        }
    }

    return generator { IterableGenerator(it, values.iterator()) }
}

/**
 * Creates a [Many] that always immediately publishes the given [values] and then completes.
 */
fun <T> Many.Companion.just(values: Sequence<T>): Many<T> {
    return generator { IterableGenerator(it, values.iterator()) }
}
