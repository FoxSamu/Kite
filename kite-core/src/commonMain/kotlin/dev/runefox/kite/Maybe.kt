package dev.runefox.kite

/**
 * A [Maybe] produces zero to one (`0..1`) value.
 */
interface Maybe<out T> : Emitter<T> {
    companion object
}
