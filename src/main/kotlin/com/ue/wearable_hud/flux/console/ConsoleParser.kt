package com.ue.wearable_hud.flux.console

interface ConsoleParser {
    fun parse(string: String): Collection<ConsoleCommand>
}

class VT100ConsoleParser : ConsoleParser {
    val ESC = "\u001B"
    val vt100EscapeSequences = Regex("(($ESC\\[|\u009b)[\u0030-\u003f]*[\u0020-\u002f]*[\u0040-\u007e])+")
    val colorSequence = Regex("$ESC\\[([0-9;]+)m")

    override fun parse(string: String): List<ConsoleCommand> {
        val vt100Commands = vt100EscapeSequences.findAll(string)
        val commands = mutableListOf<ConsoleCommand>()
        var startIndex = 0

        if (vt100Commands.count() == 0) {
            commands.add(PrintString(string))
            startIndex = string.length
        } else {
            // Extract the VT100 commands and convert them to ConsoleCommand objects
            vt100Commands.forEach { command ->
                // Add a command for the string up to the start of the next match
                if (startIndex < command.range.first) {
                    commands.add(PrintString(string.slice(startIndex until command.range.first)))
                }
                // Convert the command and add it to the commands list - if we built one!
                toCommand(command.value)?.let { commands.add(it) }
                startIndex = command.range.last + 1
            }
        }

        // Add the rest of the string
        if (startIndex < string.length) {
            commands.add(PrintString(string.slice(startIndex until string.length)))
        }

        return commands
    }

    private fun toCommand(vt100Command: String):ConsoleCommand? {
        return when {
            colorSequence.matches(vt100Command) -> toSetColorCommand(vt100Command)
            else -> null
        }
    }

    private fun toSetColorCommand(vt100Command: String): ConsoleCommand {
        val digits = Regex("[0-9]+")
        val colors = digits.findAll(vt100Command)
        val colorCodes = colors.map { it.value.toInt() }.toList()
        var resetAll = false

        var bold: Boolean? = null
        var dim: Boolean? = null
        var blink: Boolean? = null
        var hidden: Boolean? = null

        var colorSet: TextColorMode = TextColorMode.COLOR_16
        var foreground: Int? = null
        var background: Int? = null

        colorCodes.forEachIndexed { i, colorCode ->
            val prevPrevCode = colorCodes.getOrNull(i - 2)
            val prevCode = colorCodes.getOrNull(i - 1)

            if (prevPrevCode == 38 && prevCode == 5) {
                colorSet = TextColorMode.COLOR_256
                foreground = colorCode
            } else if (prevPrevCode == 48 && prevCode == 5) {
                colorSet = TextColorMode.COLOR_256
                background = colorCode
            } else {
                when (colorCode) {
                    0 -> resetAll = true

                    // Modifiers
                    1 -> bold = true
                    2 -> dim = true
//                    4 -> underline = true
                    5 -> if (prevCode != 38 && prevCode != 48) blink = true
                    8 -> hidden = true

                    // Individal resets
                    21 -> bold = false
                    22 -> dim = false
//                    24 -> underline = false
                    25 -> blink = false
                    28 -> hidden = false

                    // 16 Color Terminals
                    in 30..37 -> foreground = colorCode
                    in 90..97 -> foreground = colorCode
                    in 40..47 -> background = colorCode
                    in 100..107 -> background = colorCode
                }
            }
        }

        if (resetAll) {
            bold = false
            dim = false
            blink = false
            hidden = false
            foreground = 39
            background = 49
        }

        return SetTextColor(dim, bold, blink, hidden, colorSet, foreground, background)
    }

}