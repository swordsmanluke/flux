package com.ue.wearable_hud.flux.terminal

import com.ue.wearable_hud.flux.task.Task
import mu.KotlinLogging
import kotlin.math.max
import kotlin.math.min
import com.github.ajalt.mordant.TermColors
import com.ue.wearable_hud.flux.extensions.stripVT100Sequences
import com.ue.wearable_hud.flux.task.CommandTask

private val logger = KotlinLogging.logger {}

enum class KeyModifier {
    ALT,
    CTRL,
    SHIFT,
    META
}

enum class TerminalKey {
    ENTER,
    DELETE,
    BACKSPACE,
    ESC,
    LEFT,
    RIGHT,
    UP,
    DOWN,
    HOME,
    END,
}

class Terminal(val getCommandedTask: (String) -> Task, val makeTaskPrimary: (Task) -> Unit) {
    var cursorPos = 0
    private var command = mutableListOf<Char>()

    val commandString : String
        get() {
            val cmd = if (cursorPos >= command.size && command.size > 0) command + " " else command
            return cmd.mapIndexed {i, c ->
                if (i == cursorPos) {
                    // TODO: figure out how to _actually_ invert this instead of just knowing
                    //   that my terminal is green. Unfortunately, `t.invert` isn't working -
                    //   it has no effect on the display, making the cursor invisible. :(
                    with(TermColors(TermColors.Level.TRUECOLOR)) {
                        (black on brightGreen)(c.toString())
                    }
                } else {
                    c.toString()
                }
            }.joinToString("")
        }

    fun moveCursorAbsolute(pos: Int) {
        cursorPos = wrap(pos, 0, command.size)
    }

    fun moveCursorRelative(distance: Int) {
        cursorPos = wrap(cursorPos + distance, 0, command.size)
    }

    fun moveCursorLeft(distance:Int=1) = moveCursorRelative(-distance)
    fun moveCursorRight(distance:Int=1) = moveCursorRelative(distance)
    fun moveCursorHome() = moveCursorAbsolute(0)
    fun moveCursorEnd() = moveCursorAbsolute(command.size + 1)

    fun sendCharacter(c: Char? = null, key: TerminalKey? = null, modifiers: List<KeyModifier> = emptyList()) {
        val ctrl = modifiers.contains(KeyModifier.CTRL)
        when {
            c != null && key == null && !ctrl -> addCharacterToCommand(c)
            key != null -> handleKey(key, modifiers)
            else -> handleCombination(c, key, modifiers)
        }
    }

    private fun handleCombination(c: Char?, key: TerminalKey?, modifiers: List<KeyModifier>) {
        val ctrl = modifiers.contains(KeyModifier.CTRL)
        when {
            ctrl && c == 'u' -> clearCommand()
            else -> {}
        }
    }

    private fun handleKey(key: TerminalKey, modifiers: List<KeyModifier>) {
        when (key) {
            TerminalKey.ENTER -> { executeCommand(commandString.stripVT100Sequences()); clearCommand() }
            TerminalKey.BACKSPACE -> deleteCharAtPos(cursorPos - 1)
            TerminalKey.DELETE -> deleteCharAtPos(cursorPos)
            TerminalKey.ESC -> clearCommand()
            TerminalKey.LEFT -> moveCursorLeft()  // TODO: If CTRL down, skip by word
            TerminalKey.RIGHT -> moveCursorRight()
            TerminalKey.HOME -> moveCursorHome()
            TerminalKey.END -> moveCursorEnd()
            else -> {}
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
        cmdArray.removeIf { s -> s.isEmpty() }
        if (cmdArray.size == 0) return

        val task = getCommandedTask(cmdArray.first())
        val taskInCommand = cmdArray.first() == task.id
        val requiresCommandTask = cmdArray.size > if (taskInCommand) 1 else 0

        val cmdTask = if (requiresCommandTask) { CommandTask("terminal-${task.id}", task) }
                      else { task }

        makeTaskPrimary(cmdTask)
        logger.info { "Received command '$command'. Sending to task ${task.id}"}


        cmdTask.sendCommand(
                if (taskInCommand) {
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