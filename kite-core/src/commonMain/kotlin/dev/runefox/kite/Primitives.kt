@file:Suppress("UNCHECKED_CAST", "UNUSED")

package dev.runefox.kite

/*
 * A few extra types of primitive value wrappers like Pair, Triple and Result. Provides:
 * - Box, a 1-tuple type
 * - Option, an optional type
 * - Either, a disjunct of two types
 */

/**
 * A [Box] is a simple object that stores a value. A [Box] can store a nullable value and therefore allows passing nullable values where non-nullable
 * values are expected. A [Box] can also be used in scenarios where one needs to distinguish various different types, but where one is generic,
 * in which case [Box] would wrap the generic type. One can see a [Box] as a 1-tuple.
 *
 * [Box]es are covariant, meaning that a [Box] of type [T] is assignable to any [Box] whose type is [T] or a supertype thereof. For example, a
 * `Box<Int>` is assignable to a `Box<Number?>`.
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

fun <T> T.toBox(): Box<T> {
    return Box(this)
}

fun <T> Box<T>?.toOption(): Option<T> {
    val box = this ?: return Option.none
    return Option.some(box.value)
}

fun <T> Option<T>.toOptionalBox(): Box<T>? {
    return fold({ Box(it) }, { null })
}

inline fun <T, U> Box<T>.map(mapping: (T) -> U): Box<U> {
    return flatMap { Box(mapping(it)) }
}

inline fun <T, U> Box<T>.flatMap(mapping: (T) -> Box<U>): Box<U> {
    return fold(mapping)
}

inline fun <T, U> Box<T>.fold(mapping: (T) -> U): U {
    return mapping(value)
}

inline fun <T> Box<T>.dispatch(action: (T) -> Unit) {
    return fold(action)
}

inline fun <T> Box<T>.filter(filter: (T) -> Boolean): Option<T> {
    return if (filter(value)) Option.some(value) else Option.none
}

infix fun <A, B> Box<A>.and(other: Box<B>): Pair<A, B> {
    return value to other.value
}

fun <A, B> Box<Pair<A, B>>.transpose(): Pair<Box<A>, Box<B>> {
    return Box(value.first) to Box(value.second)
}

fun <A, B> Pair<Box<A>, Box<B>>.transpose(): Box<Pair<A, B>> {
    return Box(first.value to second.value)
}

fun <A, B, C> Box<Triple<A, B, C>>.transpose(): Triple<Box<A>, Box<B>, Box<C>> {
    return Triple(Box(value.first), Box(value.second), Box(value.third))
}

fun <A, B, C> Triple<Box<A>, Box<B>, Box<C>>.transpose(): Box<Triple<A, B, C>> {
    return Box(Triple(first.value, second.value, third.value))
}

fun <A, B> Box<Either<A, B>>.transpose(): Either<Box<A>, Box<B>> {
    return value.map({ Box(it) }, { Box(it) })
}

fun <A, B> Either<Box<A>, Box<B>>.transpose(): Box<Either<A, B>> {
    return Box(map({ it.value }, { it.value }))
}

fun <T> Box<Option<T>>.transpose(): Option<Box<T>> {
    return value.map { Box(it) }
}

fun <T> Option<Box<T>>.transpose(): Box<Option<T>> {
    return Box(map { it.value })
}

fun <T> Box<Result<T>>.transpose(): Result<Box<T>> {
    return value.map { Box(it) }
}

fun <T> Result<Box<T>>.transpose(): Box<Result<T>> {
    return Box(map { it.value })
}

fun <T> Box<T>.toList(): List<T> {
    return listOf(value)
}

fun <T> Box<T>.toSet(): Set<T> {
    return setOf(value)
}

operator fun <T> Box<T>.iterator(): Iterator<T> {
    return toList().iterator()
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

    fun value(): T {
        return if (isSome) value as T else throw NoSuchElementException("Option value is None")
    }

    fun valueOrNull(): T? {
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
        val none = Option(false, null)

        /**
         * Creates an [Option] representing **some [T]**. It is an an [Option] of type [T], which is assignable to any [Option] whose type is [T]
         * or any supertype thereof.
         */
        fun <T> some(value: T) = Option(true, value)

        /**
         * If the given value is not null, returns a [some]. If the given value is null, returns a [none].
         */
        fun <T> maybe(value: T?) = if (value == null) none else some(value)
    }
}

fun <T> T.asSome(): Option<T> {
    return Option.some(this)
}

fun <T> T?.asMaybe(): Option<T> {
    return Option.maybe(this)
}

inline fun <T, U> Option<T>.fold(present: (T) -> U, absent: () -> U): U {
    return if (this.isSome) present(value()) else absent()
}

inline fun <T, U> Option<T>.map(mapping: (T) -> U): Option<U> {
    return fold({ Option.some(mapping(it)) }, { Option.none })
}

inline fun <T, U> Option<T>.flatMap(mapping: (T) -> Option<U>): Option<U> {
    return fold({ mapping(it) }, { Option.none })
}

inline fun <T> Option<T>.filter(filter: (T) -> Boolean): Option<T> {
    return fold({ if (filter(it)) Option.none else this }, { Option.none })
}

inline fun <T> Option<T>.orElse(fallback: () -> T): T {
    return fold({ it }, fallback)
}

fun <T> Option<T>.orElse(fallback: T): T {
    return orElse { fallback }
}

inline fun <T> Option<T>.orThrow(error: () -> Throwable): T {
    return orElse { throw error() }
}

fun <T> Option<T>.orThrow(error: Throwable): T {
    return orElse { throw error }
}

fun <T> Option<T>.orThrow(message: String): T {
    return orThrow { NoSuchElementException(message) }
}

fun <T> Option<T>.orThrow(): T {
    return orThrow { NoSuchElementException("Option value is None") }
}

inline fun <T> Option<T>.toResult(error: () -> Throwable): Result<T> {
    return fold({ Result.success(it) }, { Result.failure(error()) })
}

fun <T> Option<T>.toResult(error: Throwable): Result<T> {
    return toResult { error }
}

fun <T> Option<T>.toResult(message: String): Result<T> {
    return toResult { NoSuchElementException(message) }
}

fun <T> Option<T>.toResult(): Result<T> {
    return toResult { NoSuchElementException("Option value is None") }
}

inline fun <T> Option<T>.orMaybe(fallback: () -> Option<T>): Option<T> {
    return fold({ this }, fallback)
}

fun <T> Option<T>.orMaybe(fallback: Option<T>): Option<T> {
    return orMaybe { fallback }
}

inline fun <T> Option<T>.ifPresent(handler: (T) -> Unit) {
    fold(handler) { }
}

inline fun <T> Option<T>.ifAbsent(handler: () -> Unit) {
    fold({ }, handler)
}

inline fun <T> Option<T>.dispatch(present: (T) -> Unit, absent: () -> Unit) {
    fold(present, absent)
}

fun <T> Option<T>.toList(): List<T> {
    return fold({ listOf(it) }, { emptyList() })
}

fun <T> Option<T>.toSet(): Set<T> {
    return fold({ setOf(it) }, { emptySet() })
}

operator fun <T> Option<T>.iterator(): Iterator<T> {
    return toList().iterator()
}

infix fun <T, U> Option<T>.or(other: Option<U>): Option<Either<T, U>> {
    return fold(
        { Option.some(Either.first(it)) },
        { other.fold({ Option.some(Either.second(it)) }, { Option.none }) }
    )
}

infix fun <T, U> Option<T>.and(other: Option<U>): Option<Pair<T, U>> {
    return fold(
        { x -> other.fold({ y -> Option.some(x to y) }, { Option.none }) },
        { Option.none }
    )
}

fun <T> Result<T>.maybeGet(): Option<T> {
    return fold({ Option.some(it) }, { Option.none })
}

fun <T> Result<T>.maybeException(): Option<Throwable> {
    return fold({ Option.none }, { Option.some(it) })
}

fun <T, U> Option<Pair<T, U>>.transpose(): Pair<Option<T>, Option<U>> {
    return fold(
        { Option.some(it.first) to Option.some(it.second) },
        { Option.none to Option.none }
    )
}

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
        fun <T> first(value: T) = Either(false, value, null)

        /**
         * Returns an [Either] of first type [Nothing] and second type [T]. Due covariance of [Either], you may assign this value to any [Either]
         * type whose second type is [T] or a supertype thereof. The first type will always be compatible because [Nothing] assigns to everything.
         */
        fun <T> second(value: T) = Either(false, null, value)
    }
}

fun <T> T.asFirst(): Either<T, Nothing> {
    return Either.first(this)
}

fun <T> T.asSecond(): Either<Nothing, T> {
    return Either.second(this)
}

inline fun <A, B, T> Either<A, B>.fold(first: (A) -> T, second: (B) -> T): T {
    return if (this.isSecond) second(second()) else first(first())
}

fun <T> Either<T, *>.maybeFirst(): Option<T> {
    return fold({ Option.some(it) }, { Option.none })
}

fun <T> Either<*, T>.maybeSecond(): Option<T> {
    return fold({ Option.none }, { Option.some(it) })
}

fun <A, B> Either<A, B>.toOptionPair(): Pair<Option<A>, Option<B>> {
    return maybeFirst() to maybeSecond()
}

fun <A, B> Either<A, B>.flip(): Either<B, A> {
    return fold({ Either.second(it) }, { Either.first(it) })
}

fun <T> Either<T, T>.fold(): T {
    return fold({ it }, { it })
}

inline fun <A, B> Either<A, B>.foldDown(mapping: (B) -> A): A {
    return fold({ it }, { mapping(it) })
}

inline fun <A, B> Either<A, B>.foldUp(mapping: (A) -> B): B {
    return fold({ mapping(it) }, { it })
}

inline fun <A, B, P, Q> Either<A, B>.map(first: (A) -> P, second: (B) -> Q): Either<P, Q> {
    return fold({ Either.first(first(it)) }, { Either.second(second(it)) })
}

inline fun <A, B, P, Q> Either<A, B>.flatMap(first: (A) -> Either<P, Q>, second: (B) -> Either<P, Q>): Either<P, Q> {
    return fold(first, second)
}

inline fun <A, B, T> Either<A, B>.mapFirst(mapping: (A) -> T): Either<T, B> {
    return fold({ Either.first(mapping(it)) }, { this as Either<T, B> })
}

inline fun <A, B, T> Either<A, B>.mapSecond(mapping: (B) -> T): Either<A, T> {
    return fold({ this as Either<A, T> }, { Either.second(mapping(it)) })
}

inline fun <A, B, T> Either<A, B>.flatMapFirst(mapping: (A) -> Either<T, B>): Either<T, B> {
    return fold({ mapping(it) }, { this as Either<T, B> })
}

inline fun <A, B, T> Either<A, B>.flatMapSecond(mapping: (B) -> Either<A, T>): Either<A, T> {
    return fold({ this as Either<A, T> }, { mapping(it) })
}

inline fun <T> Either<T, *>.firstOrElse(fallback: () -> T): T {
    return foldDown { fallback() }
}

fun <T> Either<T, *>.firstOrElse(fallback: T): T {
    return foldDown { fallback }
}

inline fun <T> Either<T, *>.firstOrThrow(error: () -> Throwable): T {
    return firstOrElse { throw error() }
}

fun <T> Either<T, *>.firstOrThrow(error: Throwable): T {
    return firstOrThrow { error }
}

fun <T> Either<T, *>.firstOrThrow(message: String): T {
    return firstOrThrow { NoSuchElementException(message) }
}

fun <T> Either<T, *>.firstOrThrow(): T {
    return firstOrThrow { NoSuchElementException("Either value is Second") }
}

inline fun <T> Either<T, *>.firstToResult(error: () -> Throwable): Result<T> {
    return fold({ Result.success(it) }, { Result.failure(error()) })
}

fun <T> Either<T, *>.firstToResult(error: Throwable): Result<T> {
    return firstToResult { error }
}

fun <T> Either<T, *>.firstToResult(message: String): Result<T> {
    return firstToResult { NoSuchElementException(message) }
}

fun <T> Either<T, *>.firstToResult(): Result<T> {
    return firstToResult { NoSuchElementException("Either value is Second") }
}

inline fun <T> Either<T, *>.firstOrMaybe(fallback: () -> Option<T>): Option<T> {
    return fold({ Option.some(it) }, { fallback() })
}

fun <T> Either<T, *>.firstOrMaybe(fallback: Option<T>): Option<T> {
    return fold({ Option.some(it) }, { fallback })
}

inline fun <T> Either<*, T>.secondOrElse(fallback: () -> T): T {
    return foldUp { fallback() }
}

fun <T> Either<*, T>.secondOrElse(fallback: T): T {
    return foldUp { fallback }
}

inline fun <T> Either<*, T>.secondOrThrow(error: () -> Throwable): T {
    return secondOrElse { throw error() }
}

fun <T> Either<*, T>.secondOrThrow(error: Throwable): T {
    return secondOrThrow { error }
}

fun <T> Either<*, T>.secondOrThrow(message: String): T {
    return secondOrThrow { NoSuchElementException(message) }
}

fun <T> Either<*, T>.secondOrThrow(): T {
    return secondOrThrow { NoSuchElementException("Either value is First") }
}

inline fun <T> Either<*, T>.secondToResult(error: () -> Throwable): Result<T> {
    return fold({ Result.failure(error()) }, { Result.success(it) })
}

fun <T> Either<*, T>.secondToResult(error: Throwable): Result<T> {
    return secondToResult { error }
}

fun <T> Either<*, T>.secondToResult(message: String): Result<T> {
    return secondToResult { NoSuchElementException(message) }
}

fun <T> Either<*, T>.secondToResult(): Result<T> {
    return secondToResult { NoSuchElementException("Either value is First") }
}

inline fun <T> Either<*, T>.secondOrMaybe(fallback: () -> Option<T>): Option<T> {
    return fold({ fallback() }, { Option.some(it) })
}

fun <T> Either<*, T>.secondOrMaybe(fallback: Option<T>): Option<T> {
    return fold({ fallback }, { Option.some(it) })
}

fun <T> Result<T>.toEither(): Either<T, Throwable> {
    return fold({ Either.first(it) }, { Either.second(it) })
}

fun <T> Either<T, Throwable>.toResult(): Result<T> {
    return fold({ Result.success(it) }, { Result.failure(it) })
}

inline fun <A, B> Option<A>.toEither(absent: () -> B): Either<A, B> {
    return fold({ Either.first(it) }, { Either.second(absent()) })
}

fun <A, B> Option<A>.toEither(absent: B): Either<A, B> {
    return toEither { absent }
}

fun <T> Option<T>.toEither(): Either<T, Unit> {
    return toEither(Unit)
}

fun <T> Either<T, *>.ifFirst(first: (T) -> Unit) {
    return fold(first) {}
}

fun <T> Either<*, T>.ifSecond(second: (T) -> Unit) {
    return fold({}, second)
}

fun <A, B> Either<A, B>.dispatch(first: (A) -> Unit, second: (B) -> Unit) {
    return fold(first, second)
}
