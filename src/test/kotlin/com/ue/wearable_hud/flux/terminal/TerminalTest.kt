package com.ue.wearable_hud.flux.terminal

import com.ue.wearable_hud.flux.program.FluxConfiguration
import com.ue.wearable_hud.flux.task.NullTask
import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.window.Console
import com.ue.wearable_hud.flux.window.TextView
import com.ue.wearable_hud.flux.window.Window
import com.ue.wearable_hud.flux.window.WindowManager
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test

class TerminalTest {

    @Test
    fun addsCharactersToBuffer() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')
        term.sendCharacter('c')

        assertThat(term.commandString, `is`("abc"))
    }

    @Test
    fun movingCursorBackwardChangesCursorPosition() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')
        term.sendCharacter('c')
        val curPos = term.cursorPos
        term.moveCursorRelative(-1)

        assertThat(term.cursorPos, `is`(curPos - 1))
    }

    @Test
    fun movingCursorForwardChangesCursorPosition() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')
        term.sendCharacter('c')

        term.moveCursorAbsolute(0)
        term.moveCursorRelative(1)

        assertThat(term.cursorPos, `is`(1))
    }

    @Test
    fun canSetAbsoluteCursorPosition() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')
        term.sendCharacter('c')
        term.moveCursorAbsolute(0)

        assertThat(term.cursorPos, `is`(0))
    }

    @Test
    fun canInsertCharAtCursorPosition() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('d')

        term.moveCursorAbsolute(1)
        term.sendCharacter('b')
        term.sendCharacter('c')

        assertThat(term.commandString, `is`("abcd"))
    }

    @Test
    fun cursorPositionDoesNotGoNegative() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')

        term.moveCursorRelative(-1000)

        assertThat(term.cursorPos, `is`(0))

        term.moveCursorAbsolute(-1000)

        assertThat(term.cursorPos, `is`(0))
    }

    @Test
    fun cursorPositionDoesNotGoPastEndOfString() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')

        val endPos = term.cursorPos

        term.moveCursorRelative(10)

        assertThat(term.cursorPos, `is`(endPos))

        term.moveCursorAbsolute(1000)

        assertThat(term.cursorPos, `is`(endPos))
    }

    @Test
    fun sendingEnterCharExecutesCommand() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')

        assertThat(spyTask.command, `is`(""))
        term.sendCharacter('\n')

        assertThat(spyTask.command, `is`("ab"))
    }

    @Test
    fun sendingEnterKeyExecutesCommand() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')

        assertThat(spyTask.command, `is`(""))
        term.sendCharacter(key=TerminalKey.ENTER)

        assertThat(spyTask.command, `is`("ab"))
    }

    @Test
    fun executedCommandDoesNotIncludeTaskName() {
        val term = buildEmptyTerminal()
        spyTask.id.forEach{ term.sendCharacter(it) }
        term.sendCharacter(' ')
        term.sendCharacter('a')
        term.sendCharacter('b')

        assertThat(spyTask.command, `is`(""))
        term.sendCharacter(key=TerminalKey.ENTER)

        assertThat(spyTask.command, `is`("ab"))
    }

    @Test
    fun sendingBackspaceKeyDeletesCharBeforeCursor() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')
        term.sendCharacter(key=TerminalKey.BACKSPACE)

        assertThat(term.commandString, `is`("a"))
    }

    @Test
    fun sendingBackspaceKeyDeletesAllCharacters() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')
        term.sendCharacter(key=TerminalKey.BACKSPACE)
        term.sendCharacter(key=TerminalKey.BACKSPACE)

        assertThat(term.commandString.isEmpty(), `is`(true))
        assertThat(term.commandString.isNotEmpty(), `is`(false))
    }

    @Test
    fun sendingBackspaceKeyAtStartOfStringDoesNothing() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')
        term.moveCursorAbsolute(0)
        term.sendCharacter(key=TerminalKey.BACKSPACE)

        assertThat(term.commandString, `is`("ab"))
    }

    @Test
    fun sendingDeleteKeyDeletesCharAtCursor() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')
        term.moveCursorAbsolute(0)
        term.sendCharacter(key=TerminalKey.DELETE)

        assertThat(term.commandString, `is`("b"))
    }

    @Test
    fun sendingDeleteKeyAtEndOfStringDoesNothing() {
        val term = buildEmptyTerminal()
        term.sendCharacter('a')
        term.sendCharacter('b')
        term.sendCharacter(key=TerminalKey.DELETE)

        assertThat(term.commandString, `is`("ab"))
    }

    private val spyTask = object : Task {
        var command = ""
        override val id: String
            get() = "spy_task"

        override fun nextRunAt(): Long {
            return 0
        }

        override fun readyToSchedule(): Boolean {
            return true
        }

        override fun getLines(): Collection<String> {
            return listOf(command)
        }

        override suspend fun run(): Collection<String> {
            return getLines()
        }

        override fun sendCommand(command: List<String>) {
            this.command = command.joinToString(" ")
        }
    }

    private fun buildEmptyTerminal(): Terminal {
        return Terminal({ _ -> spyTask })  // Always return spyTask as active task
    }
}
