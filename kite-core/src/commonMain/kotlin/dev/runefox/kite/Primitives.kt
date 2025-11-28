@file:Suppress("UNCHECKED_CAST", "UNUSED")

package dev.runefox.kite

class Option<out T> private constructor(
    val some: Boolean,
    private val content: T?
) {
    val none get() = !some

    fun value(): T {
        return if (some) content as T else throw NoSuchElementException("Option value is None")
    }

    fun valueOrNull(): T? {
        return if (some) content else null
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is Option<T>
                && other.some == this.some
                && if (some) other.content == this.content else true
    }

    override fun hashCode(): Int {
        return if (some) content.hashCode() else -1
    }

    override fun toString(): String {
        return if (some) {
            "Some($content)"
        } else {
            "None"
        }
    }

    companion object {
        val none = Option(false, null)

        fun <T> some(value: T) = Option(true, value)
        fun <T> maybe(value: T?) = if (value == null) none else some(value)
    }
}

inline fun <T, U> Option<T>.fold(present: (T) -> U, absent: () -> U): U {
    return if (this.some) present(value()) else absent()
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

infix fun <T> Option<T>.or(other: Option<T>): Option<T> {
    return fold({ this }, { other })
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


class Either<out L, out R> private constructor(
    val second: Boolean,
    private val content1: L?,
    private val content2: R?
) {
    val first get() = !second

    fun first(): L {
        return if (second) throw NoSuchElementException("Either value is Second") else content1 as L
    }

    fun second(): R {
        return if (second) content2 as R else throw NoSuchElementException("Either value is First")
    }

    fun firstOrNull(): L? {
        return if (second) null else content1
    }

    fun secondOrNull(): R? {
        return if (second) content2 else null
    }

    override fun equals(other: Any?): Boolean {
        return this === other || other is Either<L, R>
                && other.second == this.second
                && if (second) other.content2 == this.content2 else other.content1 == this.content1
    }

    override fun hashCode(): Int {
        return if (second) content2.hashCode().inv() else content1.hashCode()
    }

    override fun toString(): String {
        return if (second) {
            "Second($content2)"
        } else {
            "First($content1)"
        }
    }

    companion object {
        fun <T> first(value: T) = Either(false, value, null)
        fun <T> second(value: T) = Either(false, null, value)
    }
}

inline fun <L, R, T> Either<L, R>.fold(first: (L) -> T, second: (R) -> T): T {
    return if (this.second) second(second()) else first(first())
}

fun <T> Either<T, *>.maybeFirst(): Option<T> {
    return fold({ Option.some(it) }, { Option.none })
}

fun <T> Either<*, T>.maybeSecond(): Option<T> {
    return fold({ Option.none }, { Option.some(it) })
}

fun <L, R> Either<L, R>.toOptionPair(): Pair<Option<L>, Option<R>> {
    return maybeFirst() to maybeSecond()
}

fun <L, R> Either<L, R>.flip(): Either<R, L> {
    return fold({ Either.second(it) }, { Either.first(it) })
}

fun <T> Either<T, T>.fold(): T {
    return fold({ it }, { it })
}

inline fun <L, R> Either<L, R>.foldDown(mapping: (R) -> L): L {
    return fold({ it }, { mapping(it) })
}

inline fun <L, R> Either<L, R>.foldUp(mapping: (L) -> R): R {
    return fold({ mapping(it) }, { it })
}

inline fun <L, R, T, U> Either<L, R>.map(first: (L) -> T, second: (R) -> U): Either<T, U> {
    return fold({ Either.first(first(it)) }, { Either.second(second(it)) })
}

inline fun <L, R, T, U> Either<L, R>.flatMap(first: (L) -> Either<T, U> , second: (R) -> Either<T, U> ): Either<T, U> {
    return fold(first, second)
}

inline fun <L, R, T> Either<L, R>.mapFirst(mapping: (L) -> T): Either<T, R> {
    return fold({ Either.first(mapping(it)) }, { this as Either<T, R> })
}

inline fun <L, R, T> Either<L, R>.mapSecond(mapping: (R) -> T): Either<L, T> {
    return fold({ this as Either<L, T> }, { Either.second(mapping(it)) })
}

inline fun <L, R, T> Either<L, R>.flatMapFirst(mapping: (L) -> Either<T, R>): Either<T, R> {
    return fold({ mapping(it) }, { this as Either<T, R> })
}

inline fun <L, R, T> Either<L, R>.flatMapSecond(mapping: (R) -> Either<L, T>): Either<L, T> {
    return fold({ this as Either<L, T> }, { mapping(it) })
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