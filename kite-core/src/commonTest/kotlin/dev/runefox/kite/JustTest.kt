package dev.runefox.kite

import kotlin.test.Test

class JustTest {
    @Test
    fun testMono() {
        Mono.just(3).assert {
            emits(3)
            completes()
        }
    }

    @Test
    fun testMute() {
        Mute.just().assert {
            completes()
        }
    }

    @Test
    fun testMaybe0() {
        Maybe.just().assert {
            completes()
        }
    }

    @Test
    fun testMaybe1() {
        Maybe.just(3).assert {
            emits(3)
            completes()
        }
    }

    @Test
    fun testMany0() {
        Many.just().assert {
            completes()
        }
    }

    @Test
    fun testMany1() {
        Many.just(3).assert {
            emits(3)
            completes()
        }
    }

    @Test
    fun testManyN() {
        Many.just(6, 7, 4, 2).assert {
            emits(6)
            emits(7)
            emits(4)
            emits(2)
            completes()
        }
    }

    @Test
    fun testManyList() {
        Many.just(listOf(6, 7, 4, 2)).assert {
            emits(6)
            emits(7)
            emits(4)
            emits(2)
            completes()
        }
    }

    @Test
    fun testManySequence() {
        Many.just(sequenceOf(6, 7, 4, 2)).assert {
            emits(6)
            emits(7)
            emits(4)
            emits(2)
            completes()
        }
    }
}
