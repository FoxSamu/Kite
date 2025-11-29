package dev.runefox.kite

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

interface EmitterAssertionBuilder<T> {
    fun emits(item: T)
    fun completes()
    fun fails(error: (Throwable) -> Boolean)
    fun fails(error: KClass<out Throwable>)
}

fun <T> Emitter<T>.assert(builder: EmitterAssertionBuilder<T>.() -> Unit) {
    val rec = EmitterAssertionBuilderImpl<T>()
    rec.builder()
    subscribe(rec)
}


private fun interface Error {
    fun assert(error: Throwable)
}

private class EmitterAssertionBuilderImpl<T>() : EmitterAssertionBuilder<T>, Receiver<T> {
    private val expectedValues = ArrayDeque<Any>()

    override fun emits(item: T) {
        expectedValues += Box(item)
    }

    override fun completes() {
        expectedValues += Unit
    }

    override fun fails(error: (Throwable) -> Boolean) {
        expectedValues += Error {
            assertTrue(error(it), "Mismatched error")
        }
    }

    override fun fails(error: KClass<out Throwable>) {
        expectedValues += Error {
            assertTrue(error.isInstance(it), "Expected ${error.qualifiedName} got ${it::class.qualifiedName}")
        }
    }

    private lateinit var pipe: Pipe
    override fun open(pipe: Pipe) {
        pipe.requestAll()
        this.pipe = pipe
    }

    @Suppress("UNCHECKED_CAST")
    override fun receive(item: T) {
        if (expectedValues.isEmpty()) {
            return pipe.close()
        }

        try {
            when (val expected = expectedValues.removeFirst()) {
                is Box<*> -> assertEquals((expected as Box<T>).value, item)
                is Unit -> fail("Expected completion but got item: $item")
                is Error -> fail("Expected error but got item: $item")
            }
        } catch (e: Throwable) {
            pipe.close()
            throw e
        }
    }

    override fun complete() {
        if (expectedValues.isEmpty()) {
            return
        }

        when (val expected = expectedValues.removeFirst()) {
            is Box<*> -> fail("Expected ${expected.value} but got completion")
            is Unit -> {} // OK
            is Error -> fail("Expected error but got completion")
        }
    }

    override fun error(error: Throwable) {
        if (expectedValues.isEmpty()) {
            return
        }

        when (val expected = expectedValues.removeFirst()) {
            is Box<*> -> fail("Expected ${expected.value} but got completion")
            is Unit -> fail("Expected completion but got error")
            is Error -> expected.assert(error)
        }
    }
}
