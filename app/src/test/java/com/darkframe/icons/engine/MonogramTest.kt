package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.Monogram
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonogramTest {

    @Test
    fun twoWordLabelsUseOneInitialEach() {
        assertEquals("GM", Monogram.initials("Google Maps"))
        assertEquals("MT", Monogram.initials("Microsoft Teams"))
    }

    @Test
    fun singleWordLabelsUseTheFirstTwoLetters() {
        assertEquals("SP", Monogram.initials("Spotify"))
        assertEquals("X", Monogram.initials("X"))
    }

    @Test
    fun separatorsOtherThanSpacesAreHandled() {
        assertEquals("MA", Monogram.initials("my-app"))
        assertEquals("MA", Monogram.initials("my_app"))
        assertEquals("CE", Monogram.initials("com.example.app"))
    }

    @Test
    fun resultIsNeverEmptyEvenForUnusableLabels() {
        listOf("", "   ", "!!!", " ").forEach {
            assertTrue("initials of '$it' must not be blank", Monogram.initials(it).isNotBlank())
        }
    }

    @Test
    fun numbersAndNonLatinScriptsAreAccepted() {
        assertEquals("20", Monogram.initials("2048"))
        assertEquals("ВК", Monogram.initials("ВКонтакте"))
        assertEquals("微信", Monogram.initials("微信"))
    }
}
