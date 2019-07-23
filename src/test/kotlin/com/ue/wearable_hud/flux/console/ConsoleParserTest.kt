package com.ue.wearable_hud.flux.console
import com.ue.wearable_hud.flux.task.UnixTask
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ConsoleParserTest {
    val ESC = "\u001B"
    val parser = VT100ConsoleParser()

    @Test
    fun extractsExpectedCommandsFromSimple16ColorMode() {
        val simple16Color = "$ESC[37m"
        val commands = parser.parse(simple16Color)

        assertThat(commands.count(), `is`(1))
    }

    @Test
    fun extractsTextSurroundingSimple16ColorMode() {
        val simple16Color = "Hello $ESC[37m World!"
        val commands = parser.parse(simple16Color)

        val expectedCommands = listOf(
                PrintString("Hello "),
                SetTextColor(foreground = 37),
                PrintString(" World!"))

        assertThat(commands, `is`(expectedCommands))
    }

    @Test
    fun extractsColorFromSimple16ColorMode() {
        val simple16Color = "$ESC[37m"
        val commands = parser.parse(simple16Color)
        val expectedCommand = SetTextColor(foreground = 37)
        assertThat(commands.contains(expectedCommand), `is`(true))
    }

    @Test
    fun extractsColorAndBoldFrom16ColorMode() {
        val simple16Color = "$ESC[1;37m"
        val commands = parser.parse(simple16Color)
        val expectedCommand = SetTextColor(bold=true, foreground = 37)
        assertThat(commands.contains(expectedCommand), `is`(true))
    }

    @Test
    fun extractsBoldResetFromSimple16ColorMode() {
        val simple16Color = "$ESC[21;37m"
        val commands = parser.parse(simple16Color)
        val expectedCommand = SetTextColor(bold=false, foreground = 37)
        assertThat(commands.contains(expectedCommand), `is`(true))
    }

    @Test
    fun extractsColorFromSimple256ColorMode() {
        val simple256Color = "$ESC[38;5;38m"
        val commands = parser.parse(simple256Color)
        val expectedCommand = SetTextColor(colorSet = TextColorMode.COLOR_256, foreground = 38)
        assertThat(commands.contains(expectedCommand), `is`(true))
    }

    @Test
    fun extractsColorAndBoldFrom256ColorMode() {
        val complex256Color = "$ESC[1;38;5;38m"
        val commands = parser.parse(complex256Color)
        val expectedCommand = SetTextColor(colorSet = TextColorMode.COLOR_256, bold = true, foreground = 38)
        assertThat(commands.contains(expectedCommand), `is`(true))
    }

    @Test
    fun extractsTextSurroundingSimple256ColorMode() {
        val commands = parser.parse("Hello $ESC[38;5;24m World!$ESC[39m")

        val expectedCommands = listOf(
                PrintString("Hello "),
                SetTextColor(colorSet = TextColorMode.COLOR_256, foreground = 24),
                PrintString(" World!"),
                SetTextColor(foreground = 39)) // 39 = clear foreground color. Not sure how best to represent that...

        assertThat(commands, `is`(expectedCommands))
    }
}