package com.ue.wearable_hud.flux.terminal

import com.ue.wearable_hud.flux.task.Task
import mu.KotlinLogging
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

enum class KeyModifier {
    ALT,
    CTRL,
    META
}

enum class TerminalKey {
    ENTER,
    DELETE,
    BACKSPACE,
    ESC,
}

class Terminal(val getCommandedTask: (String) -> Task) {
    var cursorPos = 0
    private var command = mutableListOf<Char>()

    val commandString : String
        get() = command.joinToString("")

    fun moveCursorAbsolute(pos: Int) {
        cursorPos = wrap(pos, 0, command.size)
    }

    fun moveCursorRelative(distance: Int) {
        cursorPos = wrap(cursorPos + distance, 0, command.size)
    }

    fun sendCharacter(c: Char? = null, key: TerminalKey? = null, modifiers: List<KeyModifier> = emptyList()) {
        if (c == '\n' || key == TerminalKey.ENTER) {
            executeCommand(commandString)
            clearCommand()
        } else if (key == TerminalKey.BACKSPACE) {
            deleteCharAtPos(cursorPos - 1)
        } else if (key == TerminalKey.DELETE) {
            deleteCharAtPos(cursorPos)
        } else if (key == TerminalKey.ESC) {
            clearCommand()
        } else if (c != null){
            addCharacterToCommand(c)
        } else if (key != null) {
            throw NotImplementedError("Unhandled key: $key")
        }
    }

    private fun clearCommand() {
        command = mutableListOf()
        cursorPos = 0
    }

    private fun deleteCharAtPos(pos: Int) {
        if (pos >= 0 && pos < command.size) {
            command.removeAt(pos)
            cursorPos = wrap(pos, 0, command.size)
        }
    }

    private fun addCharacterToCommand(c: Char) {
        command.add(cursorPos, c)
        moveCursorRelative(1)
    }

    private fun wrap(value: Int, minVal: Int, maxVal: Int): Int {
        return min(max(value, minVal), maxVal)
    }

    private fun executeCommand(command: String) {
        val cmdArray = command.split(" ").toMutableList()
        if (cmdArray.size == 0) return

        val task = getCommandedTask(cmdArray.first())

        task.sendCommand(
                if (cmdArray.first() == task.id) {
                    // Don't send the 'task_id' string to the task as arguments.
                    cmdArray.slice(1 until cmdArray.size)
                } else {
                    // We omitted the task id and are just using the active task
                    cmdArray
                }.apply {
                    logger.info("Sending command to ${task.id}: ${this.joinToString (" ")}")
                }
        )

    }
}