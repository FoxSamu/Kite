package dev.runefox.kite

/*
 * MAP
 * ---
 *
 * The MAP operator creates a wrapping publisher that maps values from one type to another.
 *
 * E.g.: Mono.just(3).map { it.toString() } returns a Mono<String> that supplies "3".
 */

private class MapOperator<I, O>(
    dst: Receiver<O>,
    val map: (I) -> O
) : Operator<I, O>(dst) {
    override fun receive(item: I) {
        emit(map(item))
    }
}

fun <I, O> Mono<I>.map(map: (I) -> O): Mono<O> {
    return operator { MapOperator(it, map) }
}

fun <I, O> Maybe<I>.map(map: (I) -> O): Maybe<O> {
    return operator { MapOperator(it, map) }
}

fun <I, O> Many<I>.map(map: (I) -> O): Many<O> {
    return operator { MapOperator(it, map) }
}