@file:Suppress("UNCHECKED_CAST", "UNUSED")

package dev.runefox.kite

import kotlin.coroutines.EmptyCoroutineContext.fold

/*
 * A few extra types of primitive value wrappers like Pair, Triple and Result. Provides:
 * - Box, a 1-tuple type
 * - Option, an optional type
 * - Either, a disjunct of two types
 */

/**
 * Folds a [Pair] into a single value using the given mapping.
 */
inline fun <A, B, T> Pair<A, B>.fold(mapping: (A, B) -> T): T {
    return mapping(first, second)
}

/**
 * Maps a [Pair]'s elements individually using the given two mappings.
 */
inline fun <A, B, P, Q> Pair<A, B>.map(first: (A) -> P, second: (B) -> Q): Pair<P, Q> {
    return Pair(first(this.first), second(this.second))
}

/**
 * Maps a [Pair]'s [first][Pair.first] value using the given mapping.
 */
inline fun <A, B, T> Pair<A, B>.mapFirst(mapping: (A) -> T): Pair<T, B> {
    return Pair(mapping(first), second)
}

/**
 * Maps a [Pair]'s [second][Pair.second] value using the given mapping.
 */
inline fun <A, B, T> Pair<A, B>.mapSecond(mapping: (B) -> T): Pair<A, T> {
    return Pair(first, mapping(second))
}

/**
 * Folds a [Triple] into a single value using the given mapping.
 */
inline fun <A, B, C, T> Triple<A, B, C>.fold(mapping: (A, B, C) -> T): T {
    return mapping(first, second, third)
}

/**
 * Map's a [Triple]'s elements individually using the given three mappings.
 */
inline fun <A, B, C, P, Q, R> Triple<A, B, C>.map(first: (A) -> P, second: (B) -> Q, third: (C) -> R): Triple<P, Q, R> {
    return Triple(first(this.first), second(this.second), third(this.third))
}

/**
 * Map's a [Triple]'s [first][Triple.first] element using the given mappings.
 */
inline fun <A, B, C, T> Triple<A, B, C>.mapFirst(mapping: (A) -> T): Triple<T, B, C> {
    return Triple(mapping(first), second, third)
}

/**
 * Map's a [Triple]'s [second][Triple.second] element using the given mappings.
 */
inline fun <A, B, C, T> Triple<A, B, C>.mapSecond(mapping: (B) -> T): Triple<A, T, C> {
    return Triple(first, mapping(second), third)
}

/**
 * Map's a [Triple]'s [third][Triple.third] element using the given mappings.
 */
inline fun <A, B, C, T> Triple<A, B, C>.mapThird(mapping: (C) -> T): Triple<A, B, T> {
    return Triple(first, second, mapping(third))
}

/**
 * Flattens a [Pair] whose first element is also a [Pair] into [Triple].
 */
fun <A, B, C> Pair<Pair<A, B>, C>.flattenFirst(): Triple<A, B, C> {
    return Triple(first.first, first.second, second)
}

/**
 * Flattens a [Pair] whose second element is also a [Pair] into [Triple].
 */
fun <A, B, C> Pair<A, Pair<B, C>>.flattenSecond(): Triple<A, B, C> {
    return Triple(first, second.first, second.second)
}

/**
 * Transposes a [Pair] whose first element is a [Pair] into a [Pair] whose second element is a [Pair], without altering the order of the elements.
 */
fun <A, B, C> Pair<Pair<A, B>, C>.transposeUp(): Pair<A, Pair<B, C>> {
    return Pair(first.first, Pair(first.second, second))
}

/**
 * Transposes a [Pair] whose second element is a [Pair] into a [Pair] whose first element is a [Pair], without altering the order of the elements.
 */
fun <A, B, C> Pair<A, Pair<B, C>>.transposeDown(): Pair<Pair<A, B>, C> {
    return Pair(Pair(first, second.first), second.second)
}

/**
 * Discards the second element of this [Pair], returning an [Either] with only the first element of this pair.
 */
fun <A, B> Pair<A, B>.onlyFirst(): Either<A, Nothing> {
    return Either.first(first)
}

/**
 * Discards the first element of this [Pair], returning an [Either] with only the second element of this pair.
 */
fun <A, B> Pair<A, B>.onlySecond(): Either<Nothing, B> {
    return Either.second(second)
}

/**
 * Reverses the order of the elements in this [Pair].
 */
fun <A, B> Pair<A, B>.flip(): Pair<B, A> {
    return Pair(second, first)
}


/**
 * A [Box] is a simple object that stores a value. A [Box] can store a nullable value and therefore allows passing nullable values where non-nullable
 * values are expected. A [Box] can also be used in scenarios where one needs to distinguish various different types, but where one is generic,
 * in which case [Box] would wrap the generic type. One can see a [Box] as a 1-tuple.
 *
 * [Box]es are covariant, meaning that a [Box] of type [T] is assignable to any [Box] whose type is [T] or a supertype thereof. For example, a
 * `Box<Int>` is assignable to a `Box<Number?>`.
 *
 * @param T The type of value stored in the [Box]
 * @property value The value stored in the [Box]
 */
class Box<out T>(
    val value: T
) {
    override fun equals(other: Any?): Boolean {
        return this === other || other is Box<T> && other.value == this.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "Box($value)"
    }

    operator fun component1(): T {
        return value
    }
}

/**
 * Wraps an element into a [Box].
 */
fun <T> T.wrap(): Box<T> {
    return Box(this)
}

/**
 * Converts a nullable [Box] into a non-null [Option].
 */
fun <T> Box<T>?.toOption(): Option<T> {
    val box = this ?: return Option.none
    return Option.some(box.value)
}

/**
 * Converts a non-null [Option] into a nullable [Box].
 */
fun <T> Option<T>.toNullableBox(): Box<T>? {
    return fold({ Box(it) }, { null })
}

/**
 * Maps the value in this box.
 */
inline fun <T, U> Box<T>.map(mapping: (T) -> U): Box<U> {
    return flatMap { Box(mapping(it)) }
}

/**
 * Maps the value in this box to a [Box] and returns that [Box]. Essentially syntactic sugar for [fold] when the outcome is to be another [Box].
 */
inline fun <T, U> Box<T>.flatMap(mapping: (T) -> Box<U>): Box<U> {
    return fold(mapping)
}

/**
 * Maps the value in this [Box] and returns the mapped value.
 */
inline fun <T, U> Box<T>.fold(mapping: (T) -> U): U {
    return mapping(value)
}

/**
 * Calls the given [action] with the element in the [Box].
 */
inline fun <T> Box<T>.dispatch(action: (T) -> Unit) {
    return fold(action)
}

/**
 * Returns an [Option] that is [some][Option.some] when the given [filter] returns true, or [none][Option.none] when the given [filter] returns false.
 */
inline fun <T> Box<T>.filter(filter: (T) -> Boolean): Option<T> {
    return if (filter(value)) Option.some(value) else Option.none
}

/**
 * Joins two [Box]es into a [Pair].
 */
infix fun <A, B> Box<A>.and(other: Box<B>): Pair<A, B> {
    return value to other.value
}

/**
 * Transposes a [Box] of a [Pair] into a [Pair] of [Box]es.
 */
fun <A, B> Box<Pair<A, B>>.transpose(): Pair<Box<A>, Box<B>> {
    return Box(value.first) to Box(value.second)
}

/**
 * Transposes a [Pair] of [Box]es into [Box] of a [Pair].
 */
fun <A, B> Pair<Box<A>, Box<B>>.transpose(): Box<Pair<A, B>> {
    return Box(first.value to second.value)
}

/**
 * Transposes a [Box] of a [Triple] into a [Triple] of [Box]es.
 */
fun <A, B, C> Box<Triple<A, B, C>>.transpose(): Triple<Box<A>, Box<B>, Box<C>> {
    return Triple(Box(value.first), Box(value.second), Box(value.third))
}

/**
 * Transposes a [Triple] of [Box]es into [Box] of a [Triple].
 */
fun <A, B, C> Triple<Box<A>, Box<B>, Box<C>>.transpose(): Box<Triple<A, B, C>> {
    return Box(Triple(first.value, second.value, third.value))
}

/**
 * Transposes a [Box] of an [Either] into a [Either] of [Box]es.
 */
fun <A, B> Box<Either<A, B>>.transpose(): Either<Box<A>, Box<B>> {
    return value.map({ Box(it) }, { Box(it) })
}

/**
 * Transposes a [Either] of [Box]es into [Box] of an [Either].
 */
fun <A, B> Either<Box<A>, Box<B>>.transpose(): Box<Either<A, B>> {
    return Box(map({ it.value }, { it.value }))
}

/**
 * Transposes a [Box] of an [Option] into an [Option] of a [Box].
 */
fun <T> Box<Option<T>>.transpose(): Option<Box<T>> {
    return value.map { Box(it) }
}

/**
 * Transposes a [Option] of an [Box] into an [Box] of a [Option].
 */
fun <T> Option<Box<T>>.transpose(): Box<Option<T>> {
    return Box(map { it.value })
}

/**
 * Transposes a [Box] of an [Result] into an [Result] of a [Box].
 */
fun <T> Box<Result<T>>.transpose(): Result<Box<T>> {
    return value.map { Box(it) }
}

/**
 * Transposes a [Result] of an [Box] into an [Box] of a [Result].
 */
fun <T> Result<Box<T>>.transpose(): Box<Result<T>> {
    return Box(map { it.value })
}

/**
 * Returns a singleton [List] with the element of this [Box].
 */
fun <T> Box<T>.toList(): List<T> {
    return listOf(value)
}

/**
 * Returns a singleton [Set] with the element of this [Box].
 */
fun <T> Box<T>.toSet(): Set<T> {
    return setOf(value)
}

/**
 * Returns a singleton [MutableList] with the element of this [Box].
 */
fun <T> Box<T>.toMutableList(): MutableList<T> {
    return mutableListOf(value)
}

/**
 * Returns a singleton [MutableSet] with the element of this [Box].
 */
fun <T> Box<T>.toMutableSet(): MutableSet<T> {
    return mutableSetOf(value)
}

/**
 * Returns a singleton [Iterator] with the element of this [Box].
 */
operator fun <T> Box<T>.iterator(): Iterator<T> {
    return toList().iterator()
}

/**
 * Returns a singleton [Sequence] with the element of this [Box].
 */
fun <T> Box<T>.asSequence(): Sequence<T> {
    return Sequence { iterator() }
}

/**
 * An [Option] represents an optional value. It is either **some** value or **none**. An [Option] is similar to a nullable value, and follows similar
 * semantics. However, [Option] itself can store a nullable value, allowing a value to be either _some non-null_, _some null_, or _none_.
 *
 * [Option] is similar to Java's `java.util.Optional`. The primary difference is that [Option] allows null values whereas `Optional` does not.
 *
 * [Option]s are covariant, meaning that an [Option] of type [T] is assignable to any [Option] whose type is [T] or a supertype thereof. For example,
 * an `Option<Int>` is assignable to an `Option<Number?>`. The **none** instance is an `Option<Nothing>`, allowing it to be assignable to all
 * [Option] types.
 */
class Option<out T> private constructor(
    val isSome: Boolean,
    private val value: T?
) {
    val isNone get() = !isSome

    fun get(): T {
        return if (isSome) value as T else throw NoSuchElementException("Option value is None")
    }

    fun getOrNull(): T? {
        return if (isSome) value else null
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is Option<T>
            && other.isSome == this.isSome
            && if (isSome) other.value == this.value else true
    }

    override fun hashCode(): Int {
        return if (isSome) value.hashCode() else -1
    }

    override fun toString(): String {
        return if (isSome) {
            "Some($value)"
        } else {
            "None"
        }
    }

    companion object {
        /**
         * The only [Option] instance representing **none**. It is an [Option] of the type [Nothing]. Since [Nothing] is a subtype of every type,
         * this value is assignable to all [Option] types.
         */
        val none: Option<Nothing> = Option(false, null)

        /**
         * Creates an [Option] representing **some [T]**. It is an an [Option] of type [T], which is assignable to any [Option] whose type is [T]
         * or any supertype thereof.
         */
        fun <T> some(value: T): Option<T> = Option(true, value)

        /**
         * If the given value is not null, returns a [some]. If the given value is null, returns a [none].
         */
        fun <T> maybe(value: T?): Option<T> = if (value == null) none else some(value)
    }
}

/**
 * Wraps an element into an [Option.some].
 */
fun <T> T.wrapSome(): Option<T> {
    return Option.some(this)
}

/**
 * Wraps an element with [Option.maybe].
 */
fun <T> T?.wrapMaybe(): Option<T> {
    return Option.maybe(this)
}

/**
 * Folds this [Option] into a single value, mapping a present value using [present] and an absent value using [absent].
 */
inline fun <T, U> Option<T>.fold(present: (T) -> U, absent: () -> U): U {
    return if (this.isSome) present(get()) else absent()
}

/**
 * Maps this [Option]'s value using the given [mapping], returning a new [Option] with the mapped value.
 */
inline fun <T, U> Option<T>.map(mapping: (T) -> U): Option<U> {
    return fold({ Option.some(mapping(it)) }, { Option.none })
}

/**
 * Maps this [Option]'s value to another [Option] using the given [mapping], returning that [Option] or [none][Option.none] if this [Option] has
 * no value.
 */
inline fun <T, U> Option<T>.flatMap(mapping: (T) -> Option<U>): Option<U> {
    return fold({ mapping(it) }, { Option.none })
}

/**
 * Filters this [Option]'s value using [filter], returning [Option.none] if [filter] returns false or if this [Option] has no value.
 */
inline fun <T> Option<T>.filter(filter: (T) -> Boolean): Option<T> {
    return fold({ if (filter(it)) Option.none else this }, { Option.none })
}

/**
 * Returns this [Option]'s value, or calls the given [fallback] when this [Option] has no value.
 */
inline fun <T> Option<T>.orElse(fallback: () -> T): T {
    return fold({ it }, fallback)
}

/**
 * Returns this [Option]'s value, or throws the given [error] when this [Option] has no value.
 */
inline fun <T> Option<T>.orThrow(error: () -> Throwable): T {
    return orElse { throw error() }
}

/**
 * Returns this [Option]'s value, or throws a [NoSuchElementException] with the given message when this [Option] has no value.
 */
fun <T> Option<T>.orThrow(message: String): T {
    return orThrow { NoSuchElementException(message) }
}

/**
 * Returns this [Option]'s value, or throws a [NoSuchElementException] when this [Option] has no value.
 */
fun <T> Option<T>.orThrow(): T {
    return orThrow { NoSuchElementException("Option value is None") }
}

/**
 * Returns this [Option]'s value as a [Result.success], or returns a [Result.failure] with the given [error] when this [Option] has no value.
 */
inline fun <T> Option<T>.toResult(error: () -> Throwable): Result<T> {
    return fold({ Result.success(it) }, { Result.failure(error()) })
}

/**
 * Returns this [Option]'s value as a [Result.success], or returns a [Result.failure] of a [NoSuchElementException] with the given message
 * when this [Option] has no value.
 */
fun <T> Option<T>.toResult(message: String): Result<T> {
    return toResult { NoSuchElementException(message) }
}

/**
 * Returns this [Option]'s value as a [Result.success], or returns a [Result.failure] of a [NoSuchElementException]
 * when this [Option] has no value.
 */
fun <T> Option<T>.toResult(): Result<T> {
    return toResult { NoSuchElementException("Option value is None") }
}

/**
 * Returns this [Option] if it has a value, and otherwise returns the given [fallback] [Option].
 */
inline fun <T> Option<T>.orMaybe(fallback: () -> Option<T>): Option<T> {
    return fold({ this }, fallback)
}

/**
 * Calls the given [handler] if this [Option] has a value, passing the value as an argument to the handler.
 */
inline fun <T> Option<T>.ifSome(handler: (T) -> Unit) {
    fold(handler) { }
}

/**
 * Calls the given [handler] if this [Option] has no value.
 */
inline fun <T> Option<T>.ifNone(handler: () -> Unit) {
    fold({ }, handler)
}

/**
 * Calls [present] if this [Option] has a value, passing the value as an argument, or calls [absent] if this option has no value.
 */
inline fun <T> Option<T>.dispatch(present: (T) -> Unit, absent: () -> Unit) {
    fold(present, absent)
}

/**
 * Returns a [List] of just this [Option]'s value, if it has one, or an empty [List] otherwise.
 */
fun <T> Option<T>.toList(): List<T> {
    return fold({ listOf(it) }, { emptyList() })
}

/**
 * Returns a [Set] of just this [Option]'s value, if it has one, or an empty [Set] otherwise.
 */
fun <T> Option<T>.toSet(): Set<T> {
    return fold({ setOf(it) }, { emptySet() })
}

/**
 * Returns a [MutableList] of just this [Option]'s value, if it has one, or an empty [MutableList] otherwise.
 */
fun <T> Option<T>.toMutableList(): MutableList<T> {
    return fold({ mutableListOf(it) }, { mutableListOf() })
}

/**
 * Returns a [MutableSet] of just this [Option]'s value, if it has one, or an empty [MutableSet] otherwise.
 */
fun <T> Option<T>.toMutableSet(): MutableSet<T> {
    return fold({ mutableSetOf(it) }, { mutableSetOf() })
}

/**
 * Returns an [Iterator] of just this [Option]'s value, if it has one, or an empty [Iterator] otherwise.
 */
operator fun <T> Option<T>.iterator(): Iterator<T> {
    return toList().iterator()
}

/**
 * Returns a [Sequence] of just this [Option]'s value, if it has one, or an empty [Sequence] otherwise.
 */
fun <T> Option<T>.asSequence(): Sequence<T> {
    return Sequence { iterator() }
}

/**
 * If this and the given [Option] both have a value, returns an [Option] with a [Pair] of those values. In any other case,
 * this returns [none][Option.none].
 */
infix fun <T, U> Option<T>.and(other: Option<U>): Option<Pair<T, U>> {
    return fold(
        { x -> other.fold({ y -> Option.some(x to y) }, { Option.none }) },
        { Option.none }
    )
}

/**
 * If this [Result] has a value, returns an [Option.some] containing that value, and otherwise returns [Option.none].
 */
fun <T> Result<T>.maybeGet(): Option<T> {
    return fold({ Option.some(it) }, { Option.none })
}

/**
 * If this [Result] has an exception, returns an [Option.some] containing that exception, and otherwise returns [Option.none].
 */
fun <T> Result<T>.maybeException(): Option<Throwable> {
    return fold({ Option.none }, { Option.some(it) })
}

/**
 * Transposes an [Option] of a [Pair] into a [Pair] of [Option]s.
 */
fun <T, U> Option<Pair<T, U>>.transpose(): Pair<Option<T>, Option<U>> {
    return fold(
        { Option.some(it.first) to Option.some(it.second) },
        { Option.none to Option.none }
    )
}

/**
 * Transposes an [Either] of [Option]s into an [Option] of an [Either].
 */
fun <T, U> Either<Option<T>, Option<U>>.transpose(): Option<Either<T, U>> {
    return fold(
        { fst -> fst.map { Either.first(it) } },
        { snd -> snd.map { Either.second(it) } }
    )
}


/**
 * An [Either] represents a value that is one of two types: either the first type, [A], or the second type, [B]. Where [Pair] is the conjunct of two
 * types, [Either] is the exclusive disjunct of two types. A singe [Either] instance never represents two values.
 *
 * Both [Option] and [Result] are a special type of [Either]:
 * - [Option] can be an [Either] of `T` and [Unit] (or virtually any other constant that could represent "none").
 * - [Result] can be an [Either] of `T` and [Throwable].
 */
class Either<out A, out B> private constructor(
    val isSecond: Boolean,
    private val first: A?,
    private val second: B?
) {
    val isFirst get() = !isSecond

    fun first(): A {
        return if (isSecond) throw NoSuchElementException("Either value is Second") else first as A
    }

    fun second(): B {
        return if (isSecond) second as B else throw NoSuchElementException("Either value is First")
    }

    fun firstOrNull(): A? {
        return if (isSecond) null else first
    }

    fun secondOrNull(): B? {
        return if (isSecond) second else null
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is Either<A, B>
            && other.isSecond == this.isSecond
            && if (isSecond) other.second == this.second else other.first == this.first
    }

    override fun hashCode(): Int {
        return if (isSecond) second.hashCode().inv() else first.hashCode()
    }

    override fun toString(): String {
        return if (isSecond) {
            "Second($second)"
        } else {
            "First($first)"
        }
    }

    companion object {
        /**
         * Returns an [Either] of first type [T] and second type [Nothing]. Due covariance of [Either], you may assign this value to any [Either]
         * type whose first type is [T] or a supertype thereof. The second type will always be compatible because [Nothing] assigns to everything.
         */
        fun <T> first(value: T): Either<T, Nothing> = Either(false, value, null)

        /**
         * Returns an [Either] of first type [Nothing] and second type [T]. Due covariance of [Either], you may assign this value to any [Either]
         * type whose second type is [T] or a supertype thereof. The first type will always be compatible because [Nothing] assigns to everything.
         */
        fun <T> second(value: T): Either<Nothing, T> = Either(false, null, value)
    }
}

/**
 * Wraps this value into [Either.first].
 */
fun <T> T.wrapFirst(): Either<T, Nothing> {
    return Either.first(this)
}

/**
 * Wraps this value into [Either.second].
 */
fun <T> T.wrapSecond(): Either<Nothing, T> {
    return Either.second(this)
}

/**
 * Folds this [Either] into a single value, mapping with [first] if this is an [Either.first] or mapping with [second] if this is an [Either.second].
 */
inline fun <A, B, T> Either<A, B>.fold(first: (A) -> T, second: (B) -> T): T {
    return if (this.isSecond) second(second()) else first(first())
}

/**
 * Returns this [Either]'s first value as an [Option], or [Option.none] if this [Either] is a second value.
 */
fun <T> Either<T, *>.maybeFirst(): Option<T> {
    return fold({ Option.some(it) }, { Option.none })
}

/**
 * Returns this [Either]'s second value as an [Option], or [Option.none] if this [Either] is a first value.
 */
fun <T> Either<*, T>.maybeSecond(): Option<T> {
    return fold({ Option.none }, { Option.some(it) })
}

/**
 * Transforms this [Either] into a [Pair] of two [Option] values, one of which has the value of this [Either] and the other being [Option.none].
 */
fun <A, B> Either<A, B>.toOptionPair(): Pair<Option<A>, Option<B>> {
    return maybeFirst() to maybeSecond()
}

/**
 * Reverses the order of this [Either]. An [Either.second] becomes an [Either.first], and vice versa.
 */
fun <A, B> Either<A, B>.flip(): Either<B, A> {
    return fold({ Either.second(it) }, { Either.first(it) })
}

/**
 * Returns the value in this [Either], whose first and second value both have the same type.
 */
fun <T, A : T, B : T> Either<A, B>.fold(): T {
    return fold({ it }, { it })
}

/**
 * Returns the first value in this [Either], mapping the second value to the same type if this is [Either.second].
 */
inline fun <T, A : T, B> Either<A, B>.foldDown(mapping: (B) -> T): T {
    return fold({ it }, { mapping(it) })
}

/**
 * Returns the second value in this [Either], mapping the first value to the same type if this is [Either.first].
 */
inline fun <T, A, B : T> Either<A, B>.foldUp(mapping: (A) -> T): T {
    return fold({ mapping(it) }, { it })
}

/**
 * Maps the value in this [Either], using [first] if this is [Either.first] or using [second] if this is [Either.second].
 */
inline fun <A, B, P, Q> Either<A, B>.map(first: (A) -> P, second: (B) -> Q): Either<P, Q> {
    return fold({ Either.first(first(it)) }, { Either.second(second(it)) })
}

/**
 * Maps the value in this [Either] to a new [Either], using [first] if this is [Either.first] or using [second] if this is [Either.second].
 */
inline fun <A, B, P, Q> Either<A, B>.flatMap(first: (A) -> Either<P, Q>, second: (B) -> Either<P, Q>): Either<P, Q> {
    return fold(first, second)
}

/**
 * Maps the value in this [Either] using [mapping] if this [Either] is an [Either.first].
 */
inline fun <A, B, T> Either<A, B>.mapFirst(mapping: (A) -> T): Either<T, B> {
    return fold({ Either.first(mapping(it)) }, { this as Either<T, B> })
}

/**
 * Maps the value in this [Either] using [mapping] if this [Either] is an [Either.second].
 */
inline fun <A, B, T> Either<A, B>.mapSecond(mapping: (B) -> T): Either<A, T> {
    return fold({ this as Either<A, T> }, { Either.second(mapping(it)) })
}

/**
 * Maps the value in this [Either] to a new [Either] using [mapping] if this [Either] is an [Either.first].
 */
inline fun <A, B, T> Either<A, B>.flatMapFirst(mapping: (A) -> Either<T, B>): Either<T, B> {
    return fold({ mapping(it) }, { this as Either<T, B> })
}

/**
 * Maps the value in this [Either] to a new [Either] using [mapping] if this [Either] is an [Either.second].
 */
inline fun <A, B, T> Either<A, B>.flatMapSecond(mapping: (B) -> Either<A, T>): Either<A, T> {
    return fold({ this as Either<A, T> }, { mapping(it) })
}

/**
 * Returns the first value in this [Either] or calls [fallback] if this [Either] has a second value.
 */
inline fun <T> Either<T, *>.firstOrElse(fallback: () -> T): T {
    return foldDown { fallback() }
}

/**
 * Returns the first value in this [Either] or throws [error] if this [Either] has a second value.
 */
inline fun <T> Either<T, *>.firstOrThrow(error: () -> Throwable): T {
    return firstOrElse { throw error() }
}

/**
 * Returns the first value in this [Either] or a [NoSuchElementException] with the given [message] if this [Either] has a second value.
 */
fun <T> Either<T, *>.firstOrThrow(message: String): T {
    return firstOrThrow { NoSuchElementException(message) }
}

/**
 * Returns the first value in this [Either] or a [NoSuchElementException] if this [Either] has a second value.
 */
fun <T> Either<T, *>.firstOrThrow(): T {
    return firstOrThrow { NoSuchElementException("Either value is Second") }
}

/**
 * Returns the first value in this [Either] as a [Result.success] or returns a [Result.failure] with the given [error]
 * if this [Either] has a second value.
 */
inline fun <T> Either<T, *>.firstToResult(error: () -> Throwable): Result<T> {
    return fold({ Result.success(it) }, { Result.failure(error()) })
}

/**
 * Returns the first value in this [Either] as a [Result.success] or returns a [Result.failure] of a [NoSuchElementException] with the given [message]
 * if this [Either] has a second value.
 */
fun <T> Either<T, *>.firstToResult(message: String): Result<T> {
    return firstToResult { NoSuchElementException(message) }
}

/**
 * Returns the first value in this [Either] as a [Result.success] or returns a [Result.failure] of a [NoSuchElementException]
 * if this [Either] has a second value.
 */
fun <T> Either<T, *>.firstToResult(): Result<T> {
    return firstToResult { NoSuchElementException("Either value is Second") }
}

/**
 * Returns the first value in this [Either] as an [Option], or returns [fallback] if the [Either] has a second value.
 */
inline fun <T> Either<T, *>.firstOrMaybe(fallback: () -> Option<T>): Option<T> {
    return fold({ Option.some(it) }, { fallback() })
}

/**
 * Returns the second value in this [Either] or calls [fallback] if this [Either] has a first value.
 */
inline fun <T> Either<*, T>.secondOrElse(fallback: () -> T): T {
    return foldUp { fallback() }
}

/**
 * Returns the second value in this [Either] or throws [error] if this [Either] has a first value.
 */
inline fun <T> Either<*, T>.secondOrThrow(error: () -> Throwable): T {
    return secondOrElse { throw error() }
}

/**
 * Returns the second value in this [Either] or a [NoSuchElementException] with the given [message] if this [Either] has a first value.
 */
fun <T> Either<*, T>.secondOrThrow(message: String): T {
    return secondOrThrow { NoSuchElementException(message) }
}

/**
 * Returns the second value in this [Either] or a [NoSuchElementException] if this [Either] has a first value.
 */
fun <T> Either<*, T>.secondOrThrow(): T {
    return secondOrThrow { NoSuchElementException("Either value is First") }
}

/**
 * Returns the second value in this [Either] as a [Result.success] or returns a [Result.failure] with the given [error]
 * if this [Either] has a first value.
 */
inline fun <T> Either<*, T>.secondToResult(error: () -> Throwable): Result<T> {
    return fold({ Result.failure(error()) }, { Result.success(it) })
}

/**
 * Returns the second value in this [Either] as a [Result.success] or returns a [Result.failure] of a [NoSuchElementException] with the given [message]
 * if this [Either] has a first value.
 */
fun <T> Either<*, T>.secondToResult(message: String): Result<T> {
    return secondToResult { NoSuchElementException(message) }
}

/**
 * Returns the second value in this [Either] as a [Result.success] or returns a [Result.failure] of a [NoSuchElementException]
 * if this [Either] has a first value.
 */
fun <T> Either<*, T>.secondToResult(): Result<T> {
    return secondToResult { NoSuchElementException("Either value is First") }
}

/**
 * Returns the second value in this [Either] as an [Option], or returns [fallback] if the [Either] has a first value.
 */
inline fun <T> Either<*, T>.secondOrMaybe(fallback: () -> Option<T>): Option<T> {
    return fold({ fallback() }, { Option.some(it) })
}

/**
 * Converts a [Result] of [T] to an [Either] of [T] or [Throwable].
 */
fun <T> Result<T>.toEither(): Either<T, Throwable> {
    return fold({ Either.first(it) }, { Either.second(it) })
}

/**
 * Converts an [Either] of [T] or [Throwable] to a [Result].
 */
fun <T, E : Throwable> Either<T, E>.toResult(): Result<T> {
    return fold({ Result.success(it) }, { Result.failure(it) })
}

/**
 * If this [Option] has a value, returns [Either.first] of that value, and otherwise returns [Either.second] of [absent].
 */
inline fun <A, B> Option<A>.toEither(absent: () -> B): Either<A, B> {
    return fold({ Either.first(it) }, { Either.second(absent()) })
}

/**
 * If this [Option] has a value, returns [Either.first] of that value, and otherwise returns [Either.second] of [Unit].
 */
fun <T> Option<T>.toEither(): Either<T, Unit> {
    return toEither { }
}

/**
 * If this [Either] has a first value, calls [handler].
 */
fun <T> Either<T, *>.ifFirst(handler: (T) -> Unit) {
    return fold(handler) {}
}

/**
 * If this [Either] has a second value, calls [handler].
 */
fun <T> Either<*, T>.ifSecond(handler: (T) -> Unit) {
    return fold({}, handler)
}

/**
 * If this [Either] has a first value, calls [first], and otherwise calls [second].
 */
fun <A, B> Either<A, B>.dispatch(first: (A) -> Unit, second: (B) -> Unit) {
    return fold(first, second)
}
