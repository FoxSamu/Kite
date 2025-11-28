package dev.runefox.kite

/**
 * A [Many] produces zero to many (`0..N`) values.
 */
interface Many<out T> : Emitter<T> {
    companion object
}
