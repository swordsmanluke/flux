package com.ue.wearable_hud.flux.extensions
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.*
import org.junit.Test

class StringTest {
    val ESC = "\u001B"
    val LAST_CODE = "$ESC[39m"
    val VT100_TEST = "T$ESC[33mE$ESC[96mS$ESC[39mT$ESC[39m" // "VT100_TEST" interspersed with color codes for VT100 terminals
    val PLAIN_TEST = "TEST"

    @Test
    fun slicingVT100StringLargerThanStringReturnsString() {
        val sliced = VT100_TEST.visibleCharSlice(0..100)
        assertThat(sliced, `is`(VT100_TEST))
    }

    @Test
    fun slicingVT100StringFromHeadIntoMiddleOfString() {
        val sliced = VT100_TEST.visibleCharSlice(0..2)

        // Extracts "TE" + final VT100 code
        assertThat(sliced, `is`(VT100_TEST.slice(0..12) + LAST_CODE))
    }

    @Test
    fun slicingVT100StringFromMiddleToEndOfString() {
        val sliced = VT100_TEST.visibleCharSlice(2..4)

        // Extracts "ST" + final VT100 code
        assertThat(sliced, `is`(VT100_TEST.slice(7..VT100_TEST.length-1)))
    }

    @Test
    fun slicingPlainStringLargerThanStringReturnsString() {
        val sliced = PLAIN_TEST.visibleCharSlice(0..100)
        assertThat(sliced, `is`(PLAIN_TEST))
    }

    @Test
    fun slicingPlainStringFromHeadIntoMiddleOfString() {
        val sliced = PLAIN_TEST.visibleCharSlice(0..2)
        assertThat(sliced, `is`(PLAIN_TEST.slice(0..2)))
    }

    @Test
    fun slicingPlainStringFromMiddleToEndOfString() {
        val sliced = PLAIN_TEST.visibleCharSlice(2..3)
        assertThat(sliced, `is`(PLAIN_TEST.slice(2..3)))
    }

    // Padding End of strings

    @Test
    fun paddingAddsExpectedExtraCharsToPlainStrings() {
        val padded = "TEST".visibleCharPadEnd(5, ' ')
        assertThat(padded, `is`("TEST "))
    }

    @Test
    fun paddingAddsExpectedExtraCharsToVT100Strings() {
        val padded = VT100_TEST.visibleCharPadEnd(5, ' ')
        assertThat(padded, `is`("$VT100_TEST "))
    }

    @Test
    fun paddingAddsNoExtraCharsToVT100StringsWhenStringIsLongerThanPad() {
        val padded = VT100_TEST.visibleCharPadEnd(3, ' ')
        assertThat(padded, `is`(VT100_TEST))
    }

    @Test
    fun paddingAddsNoExtraCharsToPlainStringsWhenStringIsLongerThanPad() {
        val padded = PLAIN_TEST.visibleCharPadEnd(3, ' ')
        assertThat(padded, `is`(PLAIN_TEST))
    }
}
